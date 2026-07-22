package byransha.graph.action.search;

import byransha.graph.BNode;
import byransha.nodes.primitive.StringNode;

public class SearchRegexp extends Search {
	public StringNode regexp;

	public SearchRegexp(BNode srcNode) {
		super(srcNode);
		regexp = new StringNode(this, ".*", ".+");
	}

	@Override
	protected boolean accept(BNode n) {
		return n.describeAsJSON().toString().matches(regexp.get());
	}

	@Override
	public String whatItDoes() {
		return "search all nodes matching a given regexp";
	}
}