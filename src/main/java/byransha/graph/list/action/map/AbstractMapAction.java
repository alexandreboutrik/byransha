package byransha.graph.list.action.map;

import org.checkerframework.checker.units.qual.A;

import byransha.graph.BNode;
import byransha.graph.Category;
import byransha.graph.list.action.FunctionAction;
import byransha.graph.list.action.ListNode;

public abstract class AbstractMapAction<A extends BNode, B extends BNode>
		extends FunctionAction<ListNode<A>, ListNode<B>> {

	public AbstractMapAction(ListNode<A> input, Class<? extends Category>... cat) {
		super(input, cat);
	}

	@Override
	public void impl() {
		result = new ListNode<B>(null, "mapping to " + mapTo(), null);
		inputNode.get().forEach(n -> result.get().add(map(n)));
	}

	protected abstract B map(A n);

	@Override
	public String whatItDoes() {
		return "map nodes to their " + mapTo();
	}

	protected abstract String mapTo();
}