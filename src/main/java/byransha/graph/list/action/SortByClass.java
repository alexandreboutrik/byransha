package byransha.graph.list.action;

import byransha.graph.BNode;

public class SortByClass extends Sort {

	public SortByClass(ListNode inputNode) {
		super(inputNode);
	}

	@Override
	public int compare(BNode a, BNode b) {
		return a.getClass().getName().compareTo(b.getClass().getName());
	}

	@Override
	protected String sortBy() {
		return "class";
	}

}
