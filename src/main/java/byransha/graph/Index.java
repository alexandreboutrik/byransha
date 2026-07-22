package byransha.graph;

public abstract class Index {

	protected Index(BNode parent) {
		// super(parent);
		// TODO Auto-generated constructor stub
	}

	public abstract void add(BNode n);

	public abstract void delete(BNode n);

	public abstract String strategy();

}