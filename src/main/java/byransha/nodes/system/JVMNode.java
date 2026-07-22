package byransha.nodes.system;

import java.lang.management.ManagementFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;

import byransha.graph.BGraph;
import byransha.graph.ShowInKishanView;
import byransha.graph.list.action.ListNode;
import byransha.nodes.primitive.StringNode;

public class JVMNode extends SystemNode {

	public JVMNode(BGraph g) {
		super(g);
	}

	@Override
	public String toString() {
		return "JVM " + System.getProperty("java.version");
	}

	@Override
	public String whatIsThis() {
		return "the JVM running this application";
	}

	@ShowInKishanView
	public String version() {
		return System.getProperty("java.version");
	}

	@ShowInKishanView
	public String home() {
		return System.getProperty("java.home");
	}

	@ShowInKishanView
	public String vendor() {
		return System.getProperty("java.vm.vendor");
	}

	@ShowInKishanView
	public String name() {
		return System.getProperty("java.vm.name");
	}

	@ShowInKishanView
	public String classVersion() {
		return System.getProperty("java.class.version");
	}

	@ShowInKishanView
	public long maxHeap() {
		return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax();
	}

	@ShowInKishanView
	public long usedHeap() {
		return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
	}

	@ShowInKishanView
	public String pid() {
		String vmName = ManagementFactory.getRuntimeMXBean().getName();
		return vmName.split("@")[0];
	}

	@ShowInKishanView
	public ListNode<StringNode> props() {
		var r = new ListNode<StringNode>(this, "props", StringNode.class);

		for (var e : System.getProperties().entrySet()) {
			r.elements.add(new StringNode(this, e.getKey() + " = " + e.getValue(), null));
		}

		return r;
	}

	@Override
	public ObjectNode describeAsJSON() {
		var r = new ObjectNode(factory);
		r.put("heap size", ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());

		var props = new ObjectNode(factory);
		r.set("properties", props);

		for (var e : System.getProperties().entrySet()) {
			props.put((String) e.getKey(), (String) e.getValue());
		}

		return r;
	}

}
