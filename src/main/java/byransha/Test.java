package byransha;

import byransha.graph.BGraph;
import byransha.graph.BNode;
import byransha.graph.list.action.ListNode;

public class Test {
	public static void main(String[] args) throws Exception {
		var g = new BGraph(null, -1);
		System.out.println("creating list node");
		ListNode<BNode> l = new ListNode<>(g, "test list", BNode.class);
		System.out.println(l.contentClass);
	}
}
