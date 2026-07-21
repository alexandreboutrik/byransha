package byransha.nodes.lab.device;

import byransha.graph.BNode;
import byransha.nodes.primitive.LongNode;
import byransha.nodes.primitive.URLNode;

public class CPU extends BNode {
	LongNode nbCores;
	URLNode url;

	public CPU(BNode parent) {
		super(parent);
	}

}
