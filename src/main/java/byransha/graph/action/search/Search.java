package byransha.graph.action.search;

import byransha.graph.ShowInKishanView;
import byransha.graph.BNode;
import byransha.graph.Category;
import byransha.graph.list.action.FunctionAction;
import byransha.graph.list.action.ListNode;
import byransha.nodes.primitive.LongNode;
import byransha.nodes.primitive.LongNode.Bounds;

public class Search extends FunctionAction<BNode, ListNode> {
	@ShowInKishanView
	public LongNode depth;

	public static class search extends Category{}
	
	public Search( BNode src) {
		super( src, search.class);
		depth = new LongNode(this);
		depth.set(1L);
		depth.setBounds(new Bounds(0, 20));//src.computeLongestPathLength()));
	}

	@Override
	public void impl() {
		var list = new ListNode<>(this, "search result at depth " + depth, BNode.class);
		inputNode.bfs(depth.get(), n -> accept(n), (n, depth) -> list.elements.add(n));
		result = list;
	}

	protected boolean accept(BNode n) {
		return true;
	}

	@Override
	public String whatItDoes() {
		return "search all nodes until a given distance";
	}

	@Override
	public boolean applies() {
		return true;
	}

}