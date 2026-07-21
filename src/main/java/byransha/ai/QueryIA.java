package byransha.ai;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import javax.swing.SwingUtilities;
import org.checkerframework.checker.units.qual.g;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import byransha.ai.QueryIA.AI;
import byransha.ai.QueryIA.AiResult;
import byransha.ai.QueryIA.ResponseMode;
import byransha.ai.QueryIA.ToolEnabledAssistant;
import byransha.graph.ActionMethod;
import byransha.graph.BNode;
import byransha.graph.Category;
import byransha.graph.ShowInKishanView;
import byransha.graph.list.action.FunctionAction;
import byransha.graph.list.action.ListNode;
import byransha.network.PeerNode;
import byransha.nodes.lab.stats.DistributionNode;
import byransha.nodes.primitive.BooleanNode;
import byransha.nodes.primitive.StringNode;
import byransha.nodes.primitive.TextNode;
import byransha.nodes.system.ChatNode;
import byransha.ui.shell.Client;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;

public class QueryIA extends FunctionAction<BNode, BNode> {
	private static final ObjectMapper mapper = new ObjectMapper();
	private static final ConcurrentHashMap<String, OllamaStreamingChatModel> MODEL_CACHE = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<String, ToolEnabledAssistant> ASSISTANT_CACHE = new ConcurrentHashMap<>();
	private static final InMemoryChatMemoryStore MEMORY_STORE = new InMemoryChatMemoryStore();
	private static final int MAX_MESSAGES = 10;

	public enum ResponseMode {
		JSON_ONLY, CONVERSATION
	}

	public enum Temerature {
		LOW, MEDIUM
	}

	@ShowInKishanView
	public final StringNode prompt = new StringNode(this, "", ".+");
	public final JSONNode inputJSON;
	@ShowInKishanView
	public final TextNode info = new TextNode(this,
			"La question est envoyé a l'IA, elle peut se tromper, verifier les réponses",
			"La question est envoyé a l'IA, elle peut se tromper, verifier les réponses");

	@ShowInKishanView
	public final BooleanNode conversation = new BooleanNode(this, false);

	private static final String PRIMARY_MODEL = "granite4:tiny-h";
	private volatile ResponseMode responseMode = ResponseMode.CONVERSATION;
	private static volatile double myCurrentSpeed = 10.0;
	private static volatile double myAlpha = -1.0;
	private static volatile double myPromptLagMs = 1500.0;
	private static volatile boolean ollamaVerified = false;
	private boolean ActivateListNodeResponse = false;
	private volatile ChatNode currentChat;

	@ShowInKishanView
	private final ListNode<PeerNode> ShowPeersInfo = getPeersFromNetworkAgent();

	private ListNode<PeerNode> getPeersFromNetworkAgent() {
		try {
			ListNode<PeerNode> peerList = new ListNode<>(this, " peers", PeerNode.class);
			try {
				var peers = g().networkAgent.peers.get();
				for (var peer : peers) {
					peerList.elements.add(peer);
				}
			} catch (Exception e) {
				System.out.println("Pas de pairs disponibles, utilisation de l'instance locale d'Ollama.");
			}
			PeerNode localPeer = new PeerNode(g());
			localPeer.name = "Local Ollama (Moi)";
			localPeer.address = InetAddress.getByName("localhost");
			try {
				localPeer.publicKey = g().networkAgent.getPublicKey();
			} catch (Exception e) {
				System.out.println("Impossible de lier la clé publique au nœud local.");
			}
			peerList.elements.add(localPeer);
			return peerList;
		} catch (UnknownHostException ex) {
			System.getLogger(QueryIA.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
		}
		return null;
	}

	public static double calculerAlphaAutomatique(long totalParameters, int expertCount) {
		double activeParameters;
		if (expertCount > 0) {
			activeParameters = (totalParameters / 1_000_000_000.0) * 0.4;
		} else {
			activeParameters = totalParameters / 1_000_000_000.0;
		}
		double alpha = 1.0 + (activeParameters * 0.1);
		return Math.clamp(alpha, 1.0, 10.0);
	}

	public static double recupererAlphaDepuisOllama(String ollamaUrl, String modelName) {
		try {
			HttpClient client = HttpClient.newHttpClient();
			String jsonPayload = "{\"name\": \"" + modelName + "\"}";
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(ollamaUrl + "/api/show"))
					.POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
					.header("Content-Type", "application/json")
					.build();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			JsonNode root = mapper.readTree(response.body());
			long totalParams = root.path("model_info").path("general.parameter_count").asLong(3_000_000_000L);
			int experts = root.path("model_info").path("general.expert_count").asInt(0);
			System.out.println("Test Alpha: " + calculerAlphaAutomatique(totalParams, experts));
			return calculerAlphaAutomatique(totalParams, experts);
		} catch (Exception e) {
			System.out.println("  Impossible de lire les specs d'Ollama, alpha par défaut = 1.0");
			return 1.0;
		}
	}

	interface ToolEnabledAssistant {
		@SystemMessage("{{system}}")
		TokenStream chat(@V("system") String systemMessage, @UserMessage String userMessage);
	}

	class AI extends Category {
	}

	public QueryIA(BNode n) {
		super(n, AI.class);
		inputJSON = new JSONNode(this, n.describeAsJSON());
	}

	@Override
	public String whatItDoes() {
		return "ask AI";
	}

	@Override
	public boolean applies() {
		return true;
	}

	@Override
	public void impl() throws Throwable {
		ActivateListNodeResponse = false;
		if (this.chat instanceof ChatNode) {
			this.currentChat = (ChatNode) this.chat;
		} else if (this.parent instanceof ChatNode) {
			this.currentChat = (ChatNode) this.parent;
		} else if (Client.lastActiveChat != null) {
			this.currentChat = Client.lastActiveChat;
		} else {
			throw new IllegalStateException("QueryIA must be used within a ChatNode context");
		}

		var userQuestion = prompt.get();
		if (userQuestion == null || userQuestion.trim().isEmpty()) {
			result = new TextNode(g(), "IA response",
					"Erreur: la question envoyée à l'IA est vide.");
			return;
		}

		// (on s'exclut du Load-Balancing)
		try {
			if (myAlpha < 0) {
				myAlpha = recupererAlphaDepuisOllama("http://localhost:11434", PRIMARY_MODEL);
			}
			if (g() != null && g().networkAgent != null) {
				// On met isComputing=true avec queueSize=1
				g().networkAgent.send(new byransha.network.PeerTelemetry(myCurrentSpeed, myPromptLagMs, 1, myAlpha));
			}
		} catch (Exception e) {
		}
		String iaResponse;
		long startTime = System.currentTimeMillis();
		int[] tokensGeneratedCount = { 0 };
		try {
			var focusedNodeJson = inputNode.describeAsJSON();
			AiResult aiResult = queryIA(focusedNodeJson, userQuestion);
			iaResponse = aiResult.text;
			tokensGeneratedCount[0] = aiResult.tokenCount;
		} finally {
			// Recalcule notre score de vitesse et on l'annonce
			long durationMs = System.currentTimeMillis() - (startTime + (long) myPromptLagMs);
			if (durationMs > 0 && tokensGeneratedCount[0] > 0) {
				myCurrentSpeed = (tokensGeneratedCount[0] / (double) durationMs) * 1000.0;
				System.out.println("Test speed: " + myCurrentSpeed + " tokens/s");
			}
			try {
				if (g() != null && g().networkAgent != null) {
					// Fin du calcul : queueSize=0
					g().networkAgent
							.send(new byransha.network.PeerTelemetry(myCurrentSpeed, myPromptLagMs, 0, myAlpha));
				}
			} catch (Exception e) {
			}
		}

		// Traiter la réponse
		if (iaResponse != null) {
			if (iaResponse.contains("```json")) {
				iaResponse = iaResponse.substring(iaResponse.indexOf("```json") + 7);
				if (iaResponse.contains("```")) {
					iaResponse = iaResponse.substring(0, iaResponse.lastIndexOf("```"));
				}
			} else if (iaResponse.startsWith("```") && iaResponse.endsWith("```")) {
				iaResponse = iaResponse.substring(3, iaResponse.length() - 3);
			}
			iaResponse = iaResponse.trim();
			if (iaResponse.startsWith("[") && iaResponse.endsWith("]")) {
				try {
					JsonNode parsed = mapper.readTree(iaResponse);
					if (parsed.isArray() && parsed.size() > 0 && parsed.get(0).isTextual()) {
						ActivateListNodeResponse = true;
						System.out.println("Activation du mode ListNode pour la réponse de l'IA");
					}
				} catch (Exception e) {
					// Ignore JSON parsing errors
				}
			}
		}
		try {
			if (responseMode == ResponseMode.CONVERSATION) {
				if (ActivateListNodeResponse) {
					try {
						JsonNode parsed = mapper.readTree(iaResponse);
						var l = new ListNode<BNode>(parent, "IA numeric array", BNode.class);
						for (JsonNode value : parsed) {
							String idText = value.asText().trim();
							if (idText.isEmpty())
								continue;
							BNode realNode = g().indexes.byId.getByText(idText);
							if (realNode != null) {
								l.elements.add(realNode);
							} else {
								System.out.println("-> Aucun nœud ne possède l'ID '" + idText + "' dans le graphe.");
							}
						}

						result = l;
						return;
					} catch (Exception e) {
						// Ignore JSON parsing errors
					}
				}
			} else if (responseMode == ResponseMode.JSON_ONLY) {
				if (ActivateListNodeResponse) {
					try {
						JsonNode parsed = mapper.readTree(iaResponse);
						var l = new ListNode<BNode>(parent, "IA numeric array", BNode.class);
						for (JsonNode value : parsed) {
							String idText = value.asText().trim();
							if (idText.isEmpty())
								continue;
							BNode realNode = g().indexes.byId.getByText(idText);
							if (realNode != null) {
								l.elements.add(realNode);
							} else {
								System.out.println("-> Aucun nœud ne possède l'ID '" + idText + "' dans le graphe.");
							}
						}

						result = l;
						return;
					} catch (Exception e) {
						// Ignore JSON parsing errors
					}
				}
			} else {
				result = new TextNode(g(), "IA response", iaResponse);
				return;
			}

			var extractedJson = AiResponseAnalyser.extractFirstJsonPayload(iaResponse);
			var analysableResponse = extractedJson != null ? extractedJson : iaResponse;

			if (AiResponseAnalyser.isArrayOfNumbers(analysableResponse)) {
				JsonNode parsed = mapper.readTree(analysableResponse);
				var l = new ListNode<TextNode>(parent, "IA numeric array", TextNode.class);
				for (JsonNode value : parsed) {
					l.elements.add(new TextNode(this, "value", value.asText()));
				}
				result = l;
			} else if (AiResponseAnalyser.isDistribution(analysableResponse)) {
				var distributionNode = new DistributionNode<String>(this) {
					@Override
					public String toString() {
						return "IA distribution";
					}
				};

				JsonNode parsed = mapper.readTree(analysableResponse);
				for (var entry : parsed.properties()) {
					distributionNode.entries.addOccurence(entry.getKey(), entry.getValue().asDouble());
				}

				result = distributionNode;
			} else {
				result = new TextNode(parent, "IA response", iaResponse);
			}
		} finally {
			if (currentChat != null) {
				final ChatNode chatToAppend = currentChat;
				final BNode resToAppend = result;
				SwingUtilities.invokeLater(() -> chatToAppend.append(resToAppend));
			}
		}

	}

	public void setResponseMode(ResponseMode responseMode) {
		this.responseMode = responseMode == null ? ResponseMode.JSON_ONLY : responseMode;
	}

	public ResponseMode getResponseMode() {
		return responseMode;
	}

	static String[] buildLlmPrompt(JsonNode inputJSON, String question, ResponseMode mode, BNode inputNode) {
		var normalizedQuestion = question == null ? "" : question.trim();
		var SystemPrompt = new StringBuilder();
		SystemPrompt.append("Your personality: You are a helpful assistant specialized in exploring a data graph..\n");
		if (mode == ResponseMode.JSON_ONLY) {
			SystemPrompt.append(
					"You have access to the graph tools. Before producing the output, call the necessary tools to gather evidence. After using the tools, provide STRICTLY valid JSON .\n");
		} else {
			SystemPrompt.append("Give a conversational answer to the user \n");
		}
		var UserPrompt = new StringBuilder();
		UserPrompt.append("--- USER QUESTION ---\n");
		UserPrompt.append(normalizedQuestion).append("\n\n");
		UserPrompt.append("--- SYSTEM INSTRUCTIONS FOR GRAPH AGENT ---\n");
		UserPrompt.append("You are an AI connected to a live graph database via GraphTools.\n");
		if (inputNode != null) {
			UserPrompt.append("The current root context node is: ").append(inputNode.idAsText())
					.append(inputNode.getClass().getSimpleName()).append("\n");
		}
		UserPrompt.append("You do not know the answer until you call a tool.\n\n");
		UserPrompt.append("METHODOLOGY FOR ANY QUESTION:\n");
		UserPrompt.append("1. FIRST STEP DECISION:\n");
		UserPrompt.append("   - If you didnt find any result for your research try to use searchByDepth");
		UserPrompt.append(
				"   - If the user asks to filter people by a property (e.g. 'né à Nice', 'born in X'), DO NOT use searchByText. Immediately call 'filterMembersByProperty' using the *current root context node ID* (provided above) to get all members, the property value , and the property name.\n");
		UserPrompt.append(
				"   - If the user is looking for a specific concept or name (e.g. 'cherche le centre X'), extract the main concept and call 'searchByText' with it.\n");
		UserPrompt.append("	  - For general details about a structure, use getStructureDetails");
		UserPrompt.append(
				"3. If the user asks for members or people ('qui travaille', 'membres', 'personnes'), call 'getMembersDetails' with the ID. This tool returns names, first names  for ALL members OR the members the user asks for,  (and you will ONLY return the birth cities, and emails if and ONLY if the user asks for them for ALL members OR the members the user asks for).\n");
		UserPrompt.append("4. For general details about a node (not members), use 'getNodeDetails'.\n");
		UserPrompt.append(
				"5. Loop through ALL relevant IDs and property tools until you have collected everything requested.\n");
		UserPrompt.append(
				"6. Answer the user using ONLY the combined text and details returned by all your tool calls.\n\n");
		UserPrompt.append("7. If you dont find the main concept, try with the current node.\n");
		UserPrompt.append("CRITICAL RULES AGAINST HALLUCINATION:\n");
		UserPrompt.append("- Most questions are in French. Answer in French.\n");
		UserPrompt.append(
				"- MULTI-STEP MANDATE: NEVER assume an information (like birth city, age, etc.) is missing just because it wasn't in the first tool call. If a specific tool exists for that property, you MUST call it for each ID.\n");
		UserPrompt.append(
				"- Do not use the email informations to get the name and surname of a person. Get them from the node details.\n");
		UserPrompt.append(
				"- If the user asks for members ('membres' or 'qui travaille chez'), use 'searchByText' then 'getMembersDetails'. The getMembersDetails tool already returns nom, prénom, ville de naissance, and emails. DO NOT call getNodeDetails for listing members.\n");
		UserPrompt.append(
				"- When the users asks for \"tout\", list all that he requested, using the tools sequentially to get the data.\n");
		UserPrompt.append(
				"- Dont repeat the same information twice, for exemple if you have already given the name of a person, do not give it again when you list the members.\n");
		UserPrompt.append(
				"- STRICT RULE FOR NAMES: Output ONLY the exact names returned by the tools. NEVER invent, guess, or add a first name (prénom) if it is not explicitly written in the tool output.\n");
		UserPrompt.append(
				"- STRICT FILTERING: When asked to find people matching a condition (e.g. born in Nice), read the tool output carefully. In your final answer, ONLY list the exact people who match the condition. DO NOT list people who do not match, and do NOT mention them at all.\n");
		UserPrompt.append(
				"- EXAMPLE: If the tool returns 'Martin', you must write 'Martin'. DO NOT write 'Jean Martin' or 'Pierre Martin'. Adding an unprovided first name is strictly forbidden.\n");
		UserPrompt.append("- If a tool returns no results, say clearly that you found nothing in the database.\n");
		UserPrompt.append("- DO NOT explain your tools or say 'I don't have access'. Just give the final data.\n");

		UserPrompt.append("--- END OF INSTRUCTIONS ---\n\n");
		SystemPrompt.append("--- FINAL OUTPUT REQUIREMENT ---\n");
		if (mode == ResponseMode.CONVERSATION) {
			SystemPrompt.append("Provide a short explanation.\n");
		} else {
			SystemPrompt.append(
					"Output STRICTLY valid JSON ONLY. Do NOT output any intro text, summary, or markdown formatting like ```json.\nCRITICAL RULE: You must NOT use markdown code blocks. Start your response directly with { or [.\n");
		}

		return new String[] { SystemPrompt.toString(), UserPrompt.toString() };
	}

	public static class AiResult {
		public String text;
		public int tokenCount;

		public AiResult(String text, int tokenCount) {
			this.text = text;
			this.tokenCount = tokenCount;
		}
	}

	protected AiResult queryIA(JsonNode inputJSON, String question)
			throws Exception {

		if (!ollamaVerified) {
			if (!OllamaRequire.checkRequirements()) {
				System.out.println(" Ollama n'est pas installé impossible de faire une requête IA.");
				return new AiResult("Erreur: Ollama n'est pas installé", 0);
			}
			ollamaVerified = true;
		}

		var assistant = getOrCreateAssistant();
		var prompts = buildLlmPrompt(inputJSON, question, responseMode, inputNode);

		// Synchronous fallback wrapper since `impl()` doesn't support async streams
		// yet.
		java.util.concurrent.CompletableFuture<AiResult> future = new java.util.concurrent.CompletableFuture<>();

		long requestStartTime = System.currentTimeMillis();
		boolean[] isFirstToken = { true };

		assistant.chat(prompts[0], prompts[1])
				.onNext(token -> {
					if (isFirstToken[0]) {
						isFirstToken[0] = false;
						myPromptLagMs = System.currentTimeMillis() - requestStartTime;
						System.out.println("Test prompt lag: " + myPromptLagMs + " ms");
					}
					System.out.print(token);
					System.out.flush(); // FORCE L'AFFICHAGE IMMEDIAT DU TOKEN
				})
				.onComplete(response -> {
					System.out.println(); // newline after stream
					int tokenCount = 0;
					if (response.tokenUsage() != null && response.tokenUsage().outputTokenCount() != null) {
						tokenCount = response.tokenUsage().outputTokenCount();
					}
					future.complete(new AiResult(response.content().text(), tokenCount));
				})
				.onError(error -> {
					System.err.println("\n  Erreur pendant le stream IA : " + error.getMessage());
					error.printStackTrace();
					future.completeExceptionally(error);
				})
				.start();

		return future.join();
	}

	private ToolEnabledAssistant getOrCreateAssistant() throws IOException {
		String currentOllamaUrl = "http://localhost:11434";
		try {
			var peers = g().networkAgent.peers.get();
			if (!peers.isEmpty()) {
				byransha.network.PeerNode bestPeer = null;
				double bestScore = -1.0;

				// On cherche le pair avec le meilleur score qui n'est pas occupé
				for (var peer : peers) {
					if (peer.getScore() > bestScore) {
						bestScore = peer.getScore();
						bestPeer = peer;

					}
				}

				if (bestPeer != null && bestPeer.address != null) {
					currentOllamaUrl = "http://" + bestPeer.address.getHostAddress() + ":11434";
					System.out.println(" requete donner au pair le plus qualifié : " + bestPeer.name + " ("
							+ currentOllamaUrl + ") avec score : " + bestScore);
				}
			}
		} catch (Exception e) {
			System.out.println("Pas de pairs disponibles, utilisation de l'instance locale d'Ollama.");
		}
		final String selectedOllamaUrl = currentOllamaUrl;
		var cacheKey = selectedOllamaUrl + "|" + PRIMARY_MODEL;
		return ASSISTANT_CACHE.computeIfAbsent(cacheKey, key -> {
			var model = getOrCreateModel(selectedOllamaUrl);
			ChatMemory memory = MessageWindowChatMemory.builder()
					.maxMessages(MAX_MESSAGES)
					.chatMemoryStore(MEMORY_STORE)
					.build();
			return AiServices.builder(ToolEnabledAssistant.class)
					.streamingChatLanguageModel(model)
					.tools(new GraphTools(inputNode))
					.chatMemory(memory)
					.build();
		});
	}

	private OllamaStreamingChatModel getOrCreateModel(String ollamaUrl) {
		var cacheKey = ollamaUrl + "|" + PRIMARY_MODEL;

		return MODEL_CACHE.computeIfAbsent(cacheKey, key -> OllamaStreamingChatModel.builder()
				.baseUrl(ollamaUrl)
				.modelName(PRIMARY_MODEL)
				.numCtx(10000)
				.temperature(0.2)
				.timeout(java.time.Duration.ofMinutes(5))
				.logRequests(false) // Mettre à true pour déboguer
				.logResponses(false) // Mettre à true pour déboguer
				.build());
	}
}
