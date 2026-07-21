package byransha.network;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.PublicKey;

import byransha.graph.BGraph;
import byransha.graph.BNode;

public class PeerNode extends BNode {
	public InetAddress address;
	public PublicKey publicKey;
	public int port;
	public String name;
	public double TokensPerSecond;
	public boolean IsComputing;
	public double promptLag;
	public int queueSize;
	public double alpha = 1.0;
	private ObjectInputStream in;
	public ObjectOutputStream out;
	private Socket socket;

	public PeerNode(BGraph g, File directory) {
		super(g);
		this.name = directory.getName();
		this.publicKey = PrivateKeyUtils.
	}

	public double getTokensPerSecond() {
		return TokensPerSecond;
	}

	public double getPromptLagMsPerToken() {
		return promptLag;
	}

	public int getCurrentQueueSize() {
		return queueSize;
	}

	public double getAlpha() {
		return alpha;
	}

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

	public void setSocket(Socket socket) throws IOException {
		this.socket = socket;
		out = new ObjectOutputStream(socket.getOutputStream());
		in = new ObjectInputStream(socket.getInputStream());
	}

	public void disconnect() {
		try {
			if (in != null) {
				in.close();
				in = null;
			}
			if (out != null) {
				out.close();
				out = null;
			}
			if (socket != null) {
				socket.close();
				socket = null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
