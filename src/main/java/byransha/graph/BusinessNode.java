package byransha.graph;

import byransha.graph.list.action.ListNode;
import byransha.nodes.lab.Issue;
import byransha.nodes.primitive.ValuedNode;

public abstract class BusinessNode extends BNode {
	@ShowInKishanView
	public final ListNode<DocumentNode> documents = new ListNode<>(this, "document(s)", DocumentNode.class);

	public BusinessNode(BNode parent) {
		super(parent);
		resilient = true;
	}

	@ShowInKishanView
	public ListNode<Issue> issues() {
		return inverseRelation("issues", Issue.class, i -> i.relatedTo);
	}

	@ActionMethod
	public void reset() {
		forEachOutInFields(getClass(), BNode.class, (f, o, ro) -> {
			if (!ro) {
				try {
					var v = (BNode) f.get(this);

					if (v instanceof ValuedNode vn) {
						vn.reset();
					}
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

}
