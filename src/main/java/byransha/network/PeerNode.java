package byransha.network;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

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

	public PeerNode(BGraph g, File directory) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
		super(g);
		this.name = directory.getName();

		{
			var publicKeyFile = new File(directory, "public_key.pem");
			var publicKeyString = Files.readString(publicKeyFile.toPath());
			byte[] der = Base64.getDecoder().decode(publicKeyString);
			X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
			this.publicKey = KeyFactory.getInstance("RSA").generatePublic(spec);
		}

		{
			var ipFile = new File(directory, "ip.txt");
			var ipS = Files.readString(ipFile.toPath());
			this.address = Inet4Address.getByName(ipS);
		}
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
