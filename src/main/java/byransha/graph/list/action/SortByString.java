package byransha.graph.list.action;

import byransha.graph.BNode;

public class SortByString extends Sort {

	public SortByString(ListNode inputNode) {
		super(inputNode);
	}

	@Override
	public int compare(BNode a, BNode b) {
		return a.toString().compareTo(b.toString());
	}

	@Override
	protected String sortBy() {
		return "text value";
	}

}
