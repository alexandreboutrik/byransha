package byransha.nodes.system;

import byransha.graph.BNode;

public abstract class Role extends BNode {

	public Role(BNode parent) {
		super(parent);
	}

	public abstract boolean isAllowedToEdit(BNode n);

	public abstract boolean isAllowedToSee(BNode n);

}
