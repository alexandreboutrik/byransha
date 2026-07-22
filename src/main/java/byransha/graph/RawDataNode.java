package byransha.graph;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import byransha.nodes.primitive.PrimitiveValueNode;

public class RawDataNode extends PrimitiveValueNode<byte[]> {
	public RawDataNode(BNode g) {
		super(g);
	}

	@Override
	public String whatIsThis() {
		return "raw data";
	}

	@Override
	public String toString() {
		if (get() == null)
			return "0 bytes";
		return "no data";
	}

	@Override
	public byte[] defaultValue() {
		return new byte[0];
	}

	@Override
	protected void writeValue(byte[] v, ObjectOutput out) throws IOException {
		out.writeInt(v.length);
		out.write(v);
	}

	@Override
	protected byte[] readValue(ObjectInput in) throws IOException {
		int len = in.readInt();
		byte[] b = new byte[len];
		in.readFully(b);
		return b;
	}

}