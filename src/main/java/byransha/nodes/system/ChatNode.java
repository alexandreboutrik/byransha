package byransha.nodes.system;

import java.util.Objects;
import javax.swing.JOptionPane;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import byransha.graph.Action;
import byransha.graph.ActionMethod;
import byransha.graph.BNode;
import byransha.graph.ProcedureAction;
import byransha.graph.ShowInKishanView;
import byransha.graph.action.JumpToAnotherNode;
import byransha.graph.list.action.FunctionAction;
import byransha.graph.list.action.ListNode;
import byransha.nodes.primitive.StringNode;

public class ChatNode extends BNode {
	@ShowInKishanView
	public ListNode<BNode> nodes = new ListNode<>(this, "history", BNode.class);
	final User user;
	Boolean AlerteIA = false;

	public ChatNode(User user) {
		super(user);
		this.user = user;
		user.chats.elements.add(this);
	}

	public BNode currentNode() {
		return nodes.get().isEmpty() ? null : nodes.get().getLast();
	}

	public void append(BNode n) {
		Objects.requireNonNull(n, "cannot append null node to chat");
		System.out.println("appending " + n + " to chat " + this);
		if ("ask AI".equals(n.toString())) {
			if (AlerteIA == false) {
				afficherAlerteOllama();
				// afficherPublicKey();
			}
			AlerteIA = true;
		}

		if (!nodes.elements.isEmpty() && n == nodes.elements.getLast()) // if same node
			return;

		if (n instanceof Action action) {
			if (action.parameters().isEmpty()) {
				action.outputConsumer = feedback -> append(new StringNode(null, (String) feedback, ".*"));
				action.chat = this;
				action.execSync();

				if (action instanceof FunctionAction fa) {
					append(fa.result);
				}
			} else {
				nodes.elements.add(action);
				action.chat = this;
			}
		} 
		else {
			nodes.elements.add(n);
		}
	}
	private void afficherAlerteOllama() {
        JOptionPane.showMessageDialog(
            null, 
           "L'utilisation de l'IA sans serveur distant requiert l'installation d'Ollama ainsi que du modèle sur votre machine locale.", 
            "Configuration requise", 
            JOptionPane.INFORMATION_MESSAGE
        );
    }	

	@Override
	public void createActions() {
		cachedActions.elements.add(new Export(this));
		cachedActions.elements.add(new JumpToAnotherNode(this));
		super.createActions();
	}

	ArrayNode export() {
		ArrayNode r = new ArrayNode(factory);

		for (var n : nodes.elements) {
			var on = new ObjectNode(factory);
			r.add(on);
			on.put("id", n.id());
			on.put("toString", n.toString());

			if (n instanceof ProcedureAction action) {
				var parmNode = new ObjectNode(factory);
				on.set("parameters", parmNode);

				n.forEachOutInFields(n.getClass(), ProcedureAction.class,
						(f, o, ro) -> parmNode.put(f.getName(), o.toString()));
			}
		}

		return r;
	}

	@Override
	public String whatIsThis() {
		return "a chat";
	}

	@Override
	public String toString() {
		return user + "'s chat";
	}

	@ActionMethod
	public void showSuperNode() {
		append(g());
	}

}
