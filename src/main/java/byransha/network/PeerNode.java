package byransha.network;

import java.io.DataOutputStream;
import java.net.InetAddress;
import java.security.PublicKey;

import byransha.graph.BGraph;
import byransha.graph.BNode;

public class PeerNode extends BNode {
	public InetAddress address;
	public PublicKey publicKey;
	public int port;
	public String name;
	public DataOutputStream out;
	public double TokensPerSecond;
	public boolean IsComputing;
    public double promptLag;
    public int queueSize;
    public double alpha = 1.0;

	public PeerNode(BGraph g) {
		super(g);
	}

    public double getTokensPerSecond() { return TokensPerSecond; }
    public double getPromptLagMsPerToken() { return promptLag; }
    public int getCurrentQueueSize() { return queueSize; }
    public double getAlpha() { return alpha; }

	@Override
	public String whatIsThis() {
		return null;
	}

	@Override
	public String toString() {
		return address.getHostName() + ":" + port + "/" + peerID();
	}

	public int peerID() {
		return publicKey.hashCode();
	}

	public double getScore() {
		// calculer Score P2P
		return (TokensPerSecond * alpha) / ((1 + queueSize) * (1 + promptLag));
	}
}
