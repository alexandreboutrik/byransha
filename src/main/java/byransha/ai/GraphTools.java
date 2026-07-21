package byransha.ai;

import byransha.graph.BNode;
import byransha.graph.action.search.Search;
import byransha.graph.action.search.SearchRegexp;
import byransha.graph.action.search.SearchText;
import byransha.graph.list.action.ListNode;
import byransha.nodes.lab.Person;
import byransha.nodes.lab.Structure;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;

import java.util.LinkedHashSet;
import java.util.Set;

public class GraphTools {
	private final BNode contextNode;

	public GraphTools(BNode contextNode) {
		this.contextNode = contextNode;
	}

	@Tool("Recherche tous les nœuds dans le graphe jusqu'à une profondeur donnée. Utilise un parcours en largeur (BFS).")
	public String searchByDepth(int maxDepth) {
		System.out.println("méthode searchByDepth appelée ");
		if (maxDepth < 0 || maxDepth > 30) {
			return "Erreur: la profondeur doit être entre 0 et 30";
		}
		try {
			var search = new Search(contextNode.g());
			search.depth.set((long) maxDepth);
			search.impl();

			var result = search.result;
			if (result == null || result.elements.isEmpty()) {
				return "Aucun nœud trouvé à cette profondeur";
			}
			var response = new StringBuilder();
			response.append(String.format("Trouvé %d nœud(s) à profondeur %d:\n",
					result.elements.size(), maxDepth));

			// Limiter à 10 résultats pour ne pas surcharger le contexte
			int count = 0;
			for (var node : result.elements) {
				// if (count >= 10) {
				// response.append(String.format("... et %d autres résultats\n",
				// result.elements.size() - 10));
				// break;
				// }
				if (node instanceof BNode bnode) {
					response.append(String.format("- [%s] %s: %s\n",
							bnode.idAsText(),
							bnode.getClass().getSimpleName(),
							bnode.toString()));
				}
				count++;
			}
			return response.toString();
		} catch (Exception e) {
			return "Erreur lors de la recherche: " + e.getMessage();
		}
	}

	@Tool("Recherche des nœuds contenant un texte spécifique. ATTENTION : Ce tool ne renvoie que les noms et IDs. Pour obtenir des détails (comme la ville de naissance ou les emails), vous DEVEZ ensuite appeler les outils spécifiques (ex: getCityOfBirthOfPerson) en utilisant les IDs trouvés.")
	public String searchByText(@P("Le texte exact ou le mot-clé à rechercher dans le graphe") String searchText,
			int maxDepth) {
		System.out.println("méthode searchByText appelée ");
		String cleanSearchText = searchText
				.replace("«", "")
				.replace("»", "")
				.replace("?", "")
				.replace("\"", "")
				.replace("'", "")
				.replace("`", "")
				.replace("**", "")
				.replace("*", "")
				.replace("?", "")
				.trim();
		System.out.println("clean du text  cleanSearchText: " + cleanSearchText);

		if (cleanSearchText == null || cleanSearchText.trim().isEmpty()) {
			return "Erreur: le texte de recherche ne peut pas être vide";
		}
		try {
			synchronized (contextNode.g().indexes) {
				var elements = contextNode.g().indexes.nodesList.stream()
						.filter(n -> {
							if (n == null)
								return false;
							try {
								String id = n.idAsText() != null ? n.idAsText().toLowerCase() : "";
								String name = n.toString() != null ? n.toString().toLowerCase() : "";
								String description = n.whatIsThis() != null ? n.whatIsThis().toLowerCase() : "";
								return id.contains(cleanSearchText) || name.contains(cleanSearchText)
										|| description.contains(cleanSearchText);
							} catch (Exception e) {
								return false;
							}
						})
						.toList();

				if (elements.isEmpty()) {
					return String.format("Aucun nœud trouvé contenant '%s'", cleanSearchText);
				}
				var response = new StringBuilder();
				response.append(String.format("Trouvé %d nœud(s) contenant '%s':\n",
						elements.size(), cleanSearchText));
				int count = 0;
				for (var node : elements) {
					// if (count >= 10) {
					// response.append(String.format("... et %d autres résultats\n",
					// elements.size() - 10));
					// break;
					// }
					if (node instanceof BNode bnode) {
						response.append(String.format("- [%s] %s: %s\n",
								bnode.idAsText(),
								bnode.getClass().getSimpleName(),
								bnode.toString()));
					}
					count++;
				}
				return response.toString();
			}
		} catch (Exception e) {
			return "Erreur lors de la recherche textuelle: " + e.getMessage();
		}
	}

	@Tool("Obtient une description détaillée d'un nœud spécifique par son ID")
	public String getNodeDetails(
			@P("L'ID textuel exact du nœud (ex: 5rjAVpvRxMHI3S), sans astérisques ni balises") String nodeId) {
		System.out.println("getNodeDetails appelée");
		if (nodeId == null || nodeId.trim().isEmpty()) {
			return "Erreur: l'ID du nœud ne peut pas être vide";
		}
		String cleanNodeId = nodeId.replace("**", "")
				.replace("*", "")
				.replace("`", "")
				.replace("«", "")
				.replace("»", "")
				.replace("?", "")
				.trim();
		if (cleanNodeId.length() > 11) {
			System.out.println(" L'IA a inventé");
			cleanNodeId = cleanNodeId.substring(0, 11);
		}

		final String finalIdToSearch = cleanNodeId;
		try {
			synchronized (contextNode.g().indexes) {
				BNode targetNode = contextNode.g().indexes.nodesList.stream()
						.filter(n -> n != null && finalIdToSearch.equals(n.idAsText()))
						.findFirst()
						.orElse(null);

				if (targetNode == null) {
					return String.format("Aucun nœud trouvé dans le index global avec l'ID '%s'", finalIdToSearch);
				}
				var response = new StringBuilder();
				response.append("### INSTRUCTION STRICTE POUR L'IA : NE JAMAIS INVENTER DE PRÉNOMS ###\n");
				response.append(
						"### SI UN MEMBRE N'A QU'UN NOM, N'INVENTE PAS DE PRÉNOM EN PLUS. RECOPIE EXACTEMENT. ###\n\n");
				response.append(String.format("Détails du nœud [%s]:\n", finalIdToSearch));
				response.append(String.format("Description: %s\n", targetNode.whatIsThis()));
				// Lister les relations sortantes
				var outs = new java.util.ArrayList<String>();
				targetNode.forEachOut((out, role) -> {
					if (out != null) {
						if (out instanceof ListNode<?> listNode) {

							var listInfo = new StringBuilder();
							listInfo.append(String.format("%s -> ListNode [%s] (%d éléments)", role, out.idAsText(),
									listNode.elements.size()));
							int listCount = 0;
							for (var elem : listNode.elements) {
								// if (listCount >= 20) {
								// listInfo.append(String.format("\n ... et %d autres",
								// listNode.elements.size() - 20));
								// break;
								// }
								if (elem instanceof BNode belem) {
									listInfo.append(String.format("\n[%s] %s: %s", belem.idAsText(),
											belem.getClass().getSimpleName(), belem.toString()));
								}
								listCount++;
							}
							outs.add(listInfo.toString());
						} else {
							outs.add(String.format("%s -> [%s] %s", role, out.idAsText(), out.toString()));
						}
					}
				});
				if (!outs.isEmpty()) {
					response.append("\nRelations sortantes:\n");
					outs.forEach(s -> response.append("  - ").append(s).append("\n"));
				}
				return response.toString();
			}
		} catch (Exception e) {
			return "Erreur lors de la récupération des détails: " + e.getMessage();
		}
	}

	@Tool("cherche UNIQUEMENT les IDs des noeuds et renvoie un listNode contenant les IDs des noeuds trouvés")
	public ListNode<BNode> searchNodeIdsByText(String searchText, int maxDepth) {
		System.out.println("searchNodeIdsByText appelée");
		var result = new ListNode<BNode>(contextNode, "searchNodeIdsByText", BNode.class);
		if (searchText == null || searchText.trim().isEmpty()) {
			return result; // Retourner une liste vide
		}
		try {
			var searchLower = searchText.toLowerCase();
			var elements = contextNode.g().indexes.nodesList.stream()
					.filter(n -> {
						if (n == null)
							return false;
						try {
							String id = n.idAsText() != null ? n.idAsText().toLowerCase() : "";
							String name = n.toString() != null ? n.toString().toLowerCase() : "";
							return id.contains(searchLower) || name.contains(searchLower);
						} catch (Exception e) {
							return false;
						}
					})
					.toList();
			if (!elements.isEmpty()) {
				for (var node : elements) {
					if (node instanceof BNode bnode) {
						result.get().add(bnode);
					}
				}
			}
		} catch (Exception e) {
		}
		return result;
	}

	@Tool("Permet de lister TOUS les membres (personnes) d'une structure, d'un laboratoire ou d'un centre de recherche. Renvoie pour chaque personne : nom, prénom, ville de naissance et emails. Utiliser cet outil quand l'utilisateur demande 'qui travaille à', 'les membres de', 'les personnes de'. NE PAS utiliser getNodeDetails pour lister des membres.")
	public String getMembersDetails(
			@P("L'ID du nœud parent (ex: le centre de recherche ou la structure)") String nodeId) {
		System.out.println("getMembersDetails appelée");
		if (nodeId == null || nodeId.trim().isEmpty()) {
			return "Erreur: l'ID ne peut pas être vide";
		}
		String cleanNodeId = nodeId.replace("**", "").replace("*", "").replace("`", "").trim();
		if (cleanNodeId.length() > 11) {
			cleanNodeId = cleanNodeId.substring(0, 11);
		}
		final String finalIdToSearch = cleanNodeId;
		try {
			synchronized (contextNode.g().indexes) {
				BNode parentNode = contextNode.g().indexes.nodesList.stream()
						.filter(n -> n != null && finalIdToSearch.equals(n.idAsText()))
						.findFirst()
						.orElse(null);
				if (parentNode == null) {
					return "Aucun nœud trouvé avec cet ID.";
				}
				Set<Person> allPersons = new LinkedHashSet<>();
				collectPersons(parentNode, allPersons);

				var response = new StringBuilder();
				response.append(String.format("Membres trouvés pour %s (%d personnes) :\n", parentNode.toString(),
						allPersons.size()));
				response.append(
						"INSTRUCTION: Recopie EXACTEMENT les informations ci-dessous. NE PAS inventer de données manquantes.\n\n");

				for (Person person : allPersons) {
					response.append(extractStructuredIdentity(person));
				}

				if (allPersons.isEmpty()) {
					response.append("Aucune personne trouvée dans cette structure.\n");
				}

				return response.toString();
			}
		} catch (Exception e) {
			return "Erreur lors de l'extraction des membres : " + e.getMessage();
		}
	}

	private void collectPersons(BNode node, Set<Person> persons) {
		if (node instanceof Structure structure) {
			try {
				var members = structure.members();
				if (members != null) {
					for (var elem : members.elements) {
						if (elem instanceof Person person) {
							persons.add(person);
						}
					}
				}
			} catch (Exception e) {
				System.out.println("Erreur lors de la récupération des membres directs: " + e.getMessage());
			}
			try {
				for (var subStructure : structure.subStructures.elements) {
					collectPersons(subStructure, persons);
				}
			} catch (Exception e) {
				System.out.println("Erreur lors du parcours des sous-structures: " + e.getMessage());
			}
		}
		node.forEachOut((out, role) -> {
			if (out instanceof Person person) {
				persons.add(person);
			} else if (out instanceof ListNode<?> listNode) {
				for (var elem : listNode.elements) {
					if (elem instanceof Person person) {
						persons.add(person);
					}
				}
			}
		});
	}

	private String extractStructuredIdentity(Person person) {
		String nom = person.name != null && person.name.get() != null ? person.name.get() : "Non renseigné";
		String prenom = person.firstName != null && person.firstName.get() != null ? person.firstName.get()
				: "Non renseigné";
		String city = person.cityOfBirth != null && person.cityOfBirth.get() != null
				&& !person.cityOfBirth.get().isEmpty() ? person.cityOfBirth.get() : "Non renseignée";
		String emails = (person.emailAddresses != null && !person.emailAddresses.elements.isEmpty())
				? String.join(", ", person.emailAddresses.elements.stream().map(Object::toString).toList())
				: "Non renseigné";
		String positions = (person.positions != null && !person.positions.elements.isEmpty())
				? String.join(", ", person.positions.elements.stream().map(Object::toString).toList())
				: "Non renseigné";
		return String.format("- [ID: %s] NOM: %s | PRÉNOM: %s | VILLE DE NAISSANCE: %s | EMAIL: %s | POSITIONS: %s\n",
				person.idAsText(), nom, prenom, city, emails, positions);
	}

	// création de getStructureDetails
	@Tool("Permet d'obtenir des détails sur une structure par exemple(I3S,COMRED, SIS ect..), y compris ses sous-structures, ses membres, ses offices ect.. Utiliser cet outil quand l'utilisateur demande 'détails de la structure', 'informations sur le laboratoire', etc. ")
	public String getStructureDetails(
			@P("L'ID du nœud de la structure (ex: le centre de recherche ou la structure)") String nodeId) {
		System.out.println("getStructureDetails appelée");
		if (nodeId == null || nodeId.trim().isEmpty()) {
			return "Erreur: l'ID ne peut pas être vide";
		}
		String cleanNodeId = nodeId.replace("**", "").replace("*", "").replace("`", "").trim();
		if (cleanNodeId.length() > 11) {
			cleanNodeId = cleanNodeId.substring(0, 11);
		}
		final String finalIdToSearch = cleanNodeId;
		try {
			synchronized (contextNode.g().indexes) {
				BNode parentNode = contextNode.g().indexes.nodesList.stream()
						.filter(n -> n != null && finalIdToSearch.equals(n.idAsText()))
						.findFirst()
						.orElse(null);
				if (parentNode == null) {
					return "Aucun nœud trouvé avec cet ID.";
				}
				var response = new StringBuilder();
				response.append(String.format("Détails de la structure %s :\n", parentNode.toString()));
				response.append(parentNode.whatIsThis()).append("\n");

				// Ajouter les sous-structures
				if (parentNode instanceof Structure structure) {
					response.append(String.format("Sous-structures (%d):\n", structure.subStructures.elements.size()));
					for (var subStructure : structure.subStructures.elements) {
						response.append(String.format("- [%s] %s\n", subStructure.idAsText(), subStructure.toString()));
					}
				}

				return response.toString();
			}
		} catch (Exception e) {
			return "Erreur lors de l'extraction des détails de la structure : " + e.getMessage();
		}
	}

	@Tool("Permet de filtrer les membres d'une structure en fonction d'une propriété spécifique (ex: ville de naissance, email, etc.).")
	public String filterMembersByProperty(
			@P("L'ID du nœud parent (ex: le centre de recherche ou la structure)") String nodeId,
			@P("Le nom de la propriété à filtrer (ex: cityOfBirth, emailAddresses)") String propertyName,
			@P("La valeur de la propriété à rechercher") String propertyValue) {
		System.out.println("filterMembersByProperty appelée");
		if (nodeId == null || nodeId.trim().isEmpty()) {
			return "Erreur: l'ID ne peut pas être vide";
		}
		if (propertyName == null || propertyName.trim().isEmpty()) {
			return "Erreur: le nom de la propriété ne peut pas être vide";
		}
		if (propertyValue == null || propertyValue.trim().isEmpty()) {
			return "Erreur: la valeur de la propriété ne peut pas être vide";
		}
		String cleanNodeId = nodeId.replace("**", "").replace("*", "").replace("`", "").trim();
		if (cleanNodeId.length() > 11) {
			cleanNodeId = cleanNodeId.substring(0, 11);
		}
		final String finalIdToSearch = cleanNodeId;
		try {
			synchronized (contextNode.g().indexes) {
				BNode parentNode = contextNode.g().indexes.nodesList.stream()
						.filter(n -> n != null && finalIdToSearch.equals(n.idAsText()))
						.findFirst()
						.orElse(null);

				if (parentNode == null) {
					return "Aucun nœud trouvé avec cet ID.";
				}

				// Collect all Person nodes, traversing the Structure hierarchy
				Set<Person> allPersons = new LinkedHashSet<>();
				collectPersons(parentNode, allPersons);

				var response = new StringBuilder();
				response.append(String.format("Membres filtrés pour %s (%d personnes) :\n", parentNode.toString(),
						allPersons.size()));
				response.append(
						"INSTRUCTION: Recopie EXACTEMENT les informations ci-dessous. NE PAS inventer de données manquantes.\n\n");

				int matchCount = 0;
				for (Person person : allPersons) {
					String actualValue = getPropertyValueAsString(person, propertyName);
					if (!actualValue.isEmpty() && actualValue.toLowerCase().contains(propertyValue.toLowerCase())) {
						response.append(extractStructuredIdentity(person));
						matchCount++;
					}
				}

				if (matchCount == 0) {
					response.append("Aucun membre ne correspond à ce filtre.\n");
				}

				return response.toString();
			}
		} catch (Exception e) {
			return "Erreur lors du filtrage des membres : " + e.getMessage();
		}
	}

	private String getPropertyValueAsString(Person person, String propertyName) {
		String prop = propertyName.toLowerCase().trim();
		if (prop.contains("ville") || prop.contains("city") || prop.contains("naissance")) {
			return person.cityOfBirth != null && person.cityOfBirth.get() != null ? person.cityOfBirth.get() : "";
		}
		if (prop.contains("nom") || (prop.contains("name") && !prop.contains("first"))) {
			return person.name != null && person.name.get() != null ? person.name.get() : "";
		}
		if (prop.contains("prenom") || prop.contains("prénom") || prop.contains("first")) {
			return person.firstName != null && person.firstName.get() != null ? person.firstName.get() : "";
		}
		if (prop.contains("email") || prop.contains("mail")) {
			if (person.emailAddresses != null && !person.emailAddresses.elements.isEmpty()) {
				var sb = new StringBuilder();
				for (var email : person.emailAddresses.elements) {
					sb.append(email.toString()).append(" ");
				}
				return sb.toString();
			}
		}
		if (prop.contains("position") || prop.contains("positions") || prop.contains("poste")) {
			if (person.positions != null && !person.positions.elements.isEmpty()) {
				var sb = new StringBuilder();
				for (var position : person.positions.elements) {
					sb.append(position.toString()).append(" ");
				}
				return sb.toString();
			}
		}

		return "";
	}

}