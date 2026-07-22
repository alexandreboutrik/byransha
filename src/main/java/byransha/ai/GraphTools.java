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

    @Tool("Recherche des nœuds contenant un texte spécifique. ATTENTION : Ce tool ne renvoie que les noms et IDs. Pour obtenir des détails (comme la ville de naissance ou les emails), vous DEVEZ ensuite appeler les outils spécifiques , en utilisant les IDs trouvés.")
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
                .trim()
                .toLowerCase();
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

    @Tool("Permet de lister TOUS les membres (personnes) d'une structure, d'un laboratoire ou d'un centre de recherche. active UNIQUEMENT les filtres demandés explicitement par l'utilisateur pour économiser de la mémoire.. NE PAS utiliser getNodeDetails pour lister des membres.")
    public String getMembersDetails(
            @P("L'ID du nœud parent (ex: le centre de recherche ou la structure)") String nodeId,
            @P("Mettre à true si l'utilisateur demande explicitement les emails") boolean inclureEmails,
            @P("Mettre à true si l'utilisateur demande explicitement les villes de naissance") boolean inclureVilles,
            @P("Mettre à true si l'utilisateur veut TOUTES les informations détaillées (publications, badges, bureaux...)") boolean modeDetailsComplets
        
        ) {
        System.out.println("getMembersDetails appelée");
        System.out.println("getMembersDetails appelée avec filtres - Emails: " + inclureEmails + ", Villes: " + inclureVilles + ", Complet: " + modeDetailsComplets);
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
                
                for (Person person : allPersons) {
                    response.append(extractStructuredIdentity(person, inclureEmails, inclureVilles, modeDetailsComplets));
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

    private String extractStructuredIdentity(Person person, boolean inclureEmails, boolean inclureVilles, boolean modeDetailsComplets) {
        String nom = person.name != null && person.name.get() != null ? person.name.get() : "Non renseigné";
        String prenom = person.firstName != null && person.firstName.get() != null ? person.firstName.get(): "Non renseigné";

        var sb = new StringBuilder();
        sb.append(String.format("- [ID: %s] NOM: %s | PRÉNOM: %s", person.idAsText(), nom, prenom));

        if (inclureVilles || modeDetailsComplets) {
            String city = person.cityOfBirth != null && person.cityOfBirth.get() != null && !person.cityOfBirth.get().isEmpty() ? person.cityOfBirth.get() : "Non renseignée";
            sb.append(String.format(" | VILLE DE NAISSANCE: %s", city));
        }
        if (inclureEmails || modeDetailsComplets) {
            String emails = (person.emailAddresses != null && !person.emailAddresses.elements.isEmpty()) ? String.join(", ", person.emailAddresses.elements.stream().map(Object::toString).toList()) : "Non renseigné";
            sb.append(String.format(" | EMAIL: %s", emails));
        }
        if (modeDetailsComplets) {
            String positions = (person.positions != null && !person.positions.elements.isEmpty()) ? String.join(", ", person.positions.elements.stream().map(Object::toString).toList()) : "Non renseigné";
            String BadgeNumber = (person.badgeNumber != null && person.badgeNumber.get() != null && !person.badgeNumber.get().isEmpty()) ? person.badgeNumber.get() : "Non renseigné";
            String Website = (person.website != null && person.website.get() != null && !person.website.get().isEmpty()) ? person.website.get() : "Non renseigné";
            String Offices = (person.offices != null && !person.offices.elements.isEmpty()) ? String.join(", ", person.offices.elements.stream().map(Object::toString).toList()) : "Non renseigné";
            String Publications = (person.publications != null && !person.publications.elements.isEmpty()) ? String.join(", ", person.publications.elements.stream().map(Object::toString).toList()) : "Non renseigné";
            String ResearchActivity = (person.researchActivity != null && person.researchActivity.get() != null && !person.researchActivity.get().isEmpty()) ? person.researchActivity.get() : "Non renseignée";
            String Structure = (person.structures != null && !person.structures.elements.isEmpty()) ? String.join(", ", person.structures.elements.stream().map(Object::toString).toList()) : "Non renseignée";

            sb.append(String.format(" | POSITIONS: %s | NUMÉRO DE BADGE: %s | SITE WEB: %s | OFFICES: %s | PUBLICATIONS: %s | ACTIVITÉ DE RECHERCHE: %s | STRUCTURE: %s",
                    positions, BadgeNumber, Website, Offices, Publications, ResearchActivity, Structure));
        }
        sb.append("\n");
        return sb.toString();
    }

   
    


}