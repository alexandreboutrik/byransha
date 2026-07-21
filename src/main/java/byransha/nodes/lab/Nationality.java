package byransha.nodes.lab;

import byransha.graph.BNode;
import byransha.nodes.primitive.StringNode;

public class Nationality extends StringNode {

	public Nationality(BNode g) {
		super(g);
	}

	@Override
	public String whatIsThis() {
		return "Nationality" + (get() != null ? " " + get() : "");
	}
}
