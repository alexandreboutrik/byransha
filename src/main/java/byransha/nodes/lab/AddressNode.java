package byransha.nodes.lab;

import byransha.graph.BNode;
import byransha.nodes.lab.device.LocationNode;
import byransha.nodes.primitive.StringNode;

public class AddressNode extends BNode {
	public final StringNode text = new StringNode(this, null, ".+");
	public final LocationNode gpsLocation = new LocationNode(this);

	protected AddressNode(BNode parent) {
		super(parent);
	}

	@Override
	public String toString() {
		return text.toString();
	}
}
