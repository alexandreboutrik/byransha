
package byransha.ai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import byransha.ai.OllamaModel.ProgressUpdate;
import byransha.util.Cout;

public class OllamaModel {
	private static final int MAX_RETRIES = 3;
	private static final int RETRY_DELAY_MS = 5000; // 5 second
	private static final String PRIMARY_MODEL = "granite4:tiny-h";
	private static final String FALLBACK_MODEL = "yi-coder:1.5b";
	private static volatile ProgressListener globalProgressListener;
	public static final String FORMAT_JSON = "json";
	public static final String FORMAT_NONE = "";
	public static final float LOW = 0.2f;
	public static final float MEDIUM = 0.7f;

	@FunctionalInterface
	public interface ProgressListener {
		void onProgress(ProgressUpdate update);
	}

	public record ProgressUpdate(String message) {
	}

	public static void setGlobalProgressListener(ProgressListener listener) {
		globalProgressListener = listener;
	}

	public static boolean stopPrimaryModel() {
		return stopPrimaryModel(null);
	}

	public static boolean stopPrimaryModel(ProgressListener listener) {
		return stopModel(PRIMARY_MODEL, listener);
	}

	public static boolean stopModel(String modelName) {
		return stopModel(modelName, null);
	}

	public static boolean stopModel(String modelName, ProgressListener listener) {
		emitProgress("Arret de l'application...", listener);
		return true;
	}

	public static synchronized String chat(String prompt) throws IOException, InterruptedException, Exception {
		return chat(prompt, null, null);
	}

	public static synchronized String chat(String prompt, ProgressListener listener)
			throws IOException, InterruptedException, Exception {
		return chat(prompt, listener, null);
	}

	public static synchronized String chat(String prompt, ProgressListener listener,
			java.util.function.Consumer<String> chunkConsumer) throws IOException, InterruptedException, Exception {
		return chat(prompt, listener, chunkConsumer, FORMAT_JSON, LOW);
	}

	public static synchronized String chat(String prompt, ProgressListener listener,
			java.util.function.Consumer<String> chunkConsumer, String format, double temperature)
			throws IOException, InterruptedException, Exception {
		emitProgress("Requete IA recue", listener);
		for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
			try {
				String response = callModelWithStream(PRIMARY_MODEL, prompt, listener, chunkConsumer, format,
						temperature);
				emitProgress("Requete IA terminee", listener);
				return response;
			} catch (Exception e) {
				emitProgress("Echec tentative " + attempt + ": " + sanitizeErrorMessage(e), listener);
				if (attempt < MAX_RETRIES) {
					sleepBeforeRetry();
				}
			}
		}
		String fallbackResponse = callModelWithStream(FALLBACK_MODEL, prompt, listener, chunkConsumer, format,
				temperature);
		emitProgress("Requete IA terminee via fallback", listener);
		return fallbackResponse;
	}

	public static void initialModel(ProgressListener listener) {
		new Thread(() -> {
			try {
				emitProgress("Chargement du modèle en mémoire vive...", listener);
				var payload = java.util.Map.of(
						"model", PRIMARY_MODEL,
						"keep_alive", "24h");

				HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:11434/api/generate"))
						.header("Content-Type", "application/json")
						.POST(HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(payload)))
						.build();
				HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());

				emitProgress("Modèle IA prêt et chaud !", listener);
				System.out.println("Ollama : Modèle " + PRIMARY_MODEL + " est chargé en RAM.");

			} catch (IOException e) {
				emitProgress("Échec du pré-chargement : " + sanitizeErrorMessage(e), listener);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				emitProgress("Échec du pré-chargement : interruption", listener);
			}
		}).start();
	}

	private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
			.version(HttpClient.Version.HTTP_1_1)
			.build();

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static String callModelWithStream(String modelName, String prompt, ProgressListener listener,
			java.util.function.Consumer<String> chunkConsumer, String format, double temperature) throws Exception {
		emitProgress("Connexion...", listener);
		java.util.Map<String, Object> payload = new java.util.HashMap<>();
		payload.put("model", modelName);
		payload.put("stream", true);
		payload.put("messages", java.util.List.of(java.util.Map.of("role", "user", "content", prompt)));
		payload.put("options", java.util.Map.of(
				"low_vram", true));
		payload.put("temperature", temperature);
		payload.put("keep_alive", "24h"); // Garde le modèle en RAM pendant 24h
		if (format != null && !format.isBlank()) {
			payload.put("format", format);
		}

		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:11434/api/chat"))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(payload)))
				.build();

		// Utiliser sendAsync pour ne pas bloquer le thread principal inutilement
		return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
				.thenApply(response -> {
					StringBuilder content = new StringBuilder();
					response.body().forEach(line -> {
						try {
							JsonNode node = MAPPER.readTree(line);
							String c = node.path("message").path("content").asText("");

							if (!c.isEmpty()) {
								content.append(c);
								if (chunkConsumer != null)
									chunkConsumer.accept(c);
							}
						} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
							Cout.warning("Ligne Ollama JSON ignoree: " + sanitizeErrorMessage(e));
						}
					});
					return content.toString();
				}).join();
	}

	private static void sleepBeforeRetry() {
		try {
			Thread.sleep(RETRY_DELAY_MS);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
	}

	private static String sanitizeErrorMessage(Exception e) {
		String msg = e.getMessage();
		return msg == null || msg.isBlank() ? e.getClass().getSimpleName() : msg;
	}

	private static void emitProgress(String message, ProgressListener localListener) {
		ProgressUpdate update = new ProgressUpdate(message);
		Cout.progress(message);
		notifyListener(localListener, update);
		notifyListener(globalProgressListener, update);
	}

	private static void notifyListener(ProgressListener listener, ProgressUpdate update) {
		if (listener == null) {
			return;
		}
		try {
			listener.onProgress(update);
		} catch (Exception listenerException) {
			Cout.warning("Echec listener progression IA: " + sanitizeErrorMessage(listenerException));
		}
	}

}