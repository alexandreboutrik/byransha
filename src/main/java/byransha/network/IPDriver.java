package byransha.network;

import java.io.FileNotFoundException;
import java.io.IOException;

import byransha.graph.BNode;

public abstract class IPDriver extends BNode {
	public final int port;
	protected int packetReceived;
	protected int packetSent;

	public IPDriver(NetworkAgent g, int port) throws FileNotFoundException, IOException {
		super(g);
		this.port = port;
	}

	public NetworkAgent na() {
		return (NetworkAgent) parent;
	}

	protected void updateInOutInfo() {
		na().inOutInfo.set(packetReceived + " received, " + packetSent + " sent");
	}

	@Override
	public String whatIsThis() {
		return protocol() + " driver";
	}

	protected abstract String protocol();

	public abstract void send(byte[] msgBytes, PeerNode to) throws IOException;
}
