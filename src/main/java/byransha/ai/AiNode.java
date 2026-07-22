package byransha.ai;

import java.net.InetAddress;
import byransha.graph.BGraph;
import byransha.graph.BNode;

public class AiNode extends BNode {
	public InetAddress address;
	public int port;
	public String name;
	public double TokensPerSecond;
	public boolean IsComputing;
    public double promptLag;
    public int queueSize;
    public double alpha = 1.0;

	public AiNode(BGraph g) {
		super(g);
	}

    public double getTokensPerSecond() { return TokensPerSecond; }
    public double getPromptLagMsPerToken() { return promptLag; }
    public int getCurrentQueueSize() { return queueSize; }
    public double getAlpha() { return alpha; }

	@Override
	public String whatIsThis() {
		return "AI node";
	}

	@Override
	public String toString() {
		return name + (address != null ? " (" + address.getHostName() + ":" + port + ")" : "");
	}

	public double getScore() {
		// calculer Score
		return (TokensPerSecond * alpha) / ((1 + queueSize) * (1 + promptLag));
	}
}
