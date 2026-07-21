package byransha.network;

import java.io.Serializable;

public class PeerTelemetry implements Serializable {
	private static final long serialVersionUID = 1L;

	public double tokensPerSecond;
	public boolean isComputing;
	public double promptLag;
	public int queueSize;
	public double alpha;

	public PeerTelemetry(double tokensPerSecond, boolean isComputing) {
		this.tokensPerSecond = tokensPerSecond;
		this.isComputing = isComputing;
		this.alpha = 1.0;
	}

	public PeerTelemetry(double tokensPerSecond, double promptLag, int queueSize, double alpha) {
		this.tokensPerSecond = tokensPerSecond;
		this.promptLag = promptLag;
		this.queueSize = queueSize;
		this.isComputing = queueSize > 0;
		this.alpha = alpha;
	}
}