package byransha.nodes.system;

import byransha.graph.BGraph;
import byransha.graph.BNode;
import byransha.graph.ShowInKishanView;
import byransha.graph.list.action.ListNode;
import byransha.nodes.primitive.StringNode;

public class User extends BNode {
	@ShowInKishanView
	public final StringNode name;
	@ShowInKishanView
	public final StringNode passwordNode = new StringNode(this, null, ".+");
	@ShowInKishanView
	public final ListNode<ChatNode> chats = new ListNode<>(this, "chats", ChatNode.class);
	@ShowInKishanView
	public final ListNode<Role> roles = new ListNode<>(this, "roles", Role.class);

	public User(BGraph g, String userName) {
		super(g);
		name = new StringNode(g, userName, ".+");
		passwordNode.hideText = true;
	}

	@Override
	public String whatIsThis() {
		return "a user of the system";
	}

	public boolean accept(String username, String p) {
		return name.get().equals(username) && passwordNode.get().equals(p);
	}

	@Override
	public String toString() {
		return name.get();
	}

	public static interface JumpListener {
		void newNode(BNode n);
	}

}
