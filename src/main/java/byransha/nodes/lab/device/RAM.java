package byransha.nodes.lab.device;

import byransha.graph.BNode;
import byransha.nodes.primitive.LongNode;

public class RAM extends BNode {
	LongNode size;
	LongNode frequency;

	public RAM(BNode parent) {
		super(parent);
	}

}
