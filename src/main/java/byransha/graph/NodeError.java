package byransha.graph;

public class NodeError {
	final public BNode node;
	final public String msg;

	public NodeError(BNode node, String msg) {
		this.node = node;
		this.msg = msg;
	}

	@Override
	public String toString() {
		return "error in " + node + ": " + msg;
	}
}
