package byransha.network;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import byransha.event.Event;
import byransha.graph.Ack;
import byransha.graph.BGraph;
import byransha.graph.BNode;
import byransha.graph.ShowInKishanView;
import byransha.graph.list.action.ListNode;
import byransha.nodes.primitive.StringNode;
import byransha.nodes.system.Byransha;
import byransha.security.RSA;
import byransha.util.GZip;
import toools.io.ser.JavaSerializer;
import toools.io.ser.Serializer;

public class NetworkAgent extends BNode {
	public static final int port = 9876;
	@ShowInKishanView
	public static final File peersDirectory = new File(Byransha.homeDirectory, "peers");

	@ShowInKishanView
	File securityDir = new File(Byransha.homeDirectory, "security");
	@ShowInKishanView
	File authorizedKeys = new File(securityDir, "authorized_keys");

	@ShowInKishanView
	final StringNode publicKeyInfo = new StringNode(this);
	@ShowInKishanView
	final StringNode inOutInfo = new StringNode(this);
	@ShowInKishanView
	public final ListNode<PeerNode> peers = new ListNode<>(this, "peers", PeerNode.class);
	@ShowInKishanView
	String peerName;
	private int nbMessagesReceived;
	private int packetSent;
	private PublicKey publicKey;
	private PrivateKey privateKey;
	IPDriver tcpDriver;
	final Serializer serializer = new JavaSerializer<>();

	public NetworkAgent(BGraph g, int tcpPort)
			throws FileNotFoundException, IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		super(g);
		this.tcpDriver = new TCPDriver(this, tcpPort);
		File publicKeyFile = new File(securityDir, "public_key.ser");
		File privateKeyFile = new File(securityDir, "private_key.ser");

		if (publicKeyFile.exists() && privateKeyFile.exists()) {
			this.publicKey = (PublicKey) RSA.fromPem(Files.readString(publicKeyFile.toPath()));
			this.privateKey = (PrivateKey) RSA.fromPem(Files.readString(privateKeyFile.toPath()));
		} else {
			System.out.println("Generating new random RSA keys");
			var keyPair = RSA.randomKeyPair();
			this.publicKey = keyPair.getPublic();
			this.privateKey = keyPair.getPrivate();
			publicKeyFile.getParentFile().mkdirs();
			Files.writeString(publicKeyFile.toPath(), RSA.toPem(publicKey));
			Files.writeString(privateKeyFile.toPath(), RSA.toPem(privateKey));
			var pub = new String(RSA.toBase64(keyPair.getPublic()));
			System.out.println("public key: " + pub);
			publicKeyInfo.set(pub);
		}

		new Thread(() -> {
			peersDirectory.mkdirs();

			while (true) {
				try {
					for (File peerDirectory : peersDirectory.listFiles()) {
						if (peerDirectory.isDirectory()) {
							var peer = findPeer(peerDirectory.getName());

							if (peer == null) {
								try {
									peer = new PeerNode(g, peerDirectory);
									peers.elements.add(peer);
								} catch (InvalidKeySpecException | NoSuchAlgorithmException | IOException e) {
									e.printStackTrace();
								}
							}
						}
					}

					Thread.sleep(990);
				} catch (InterruptedException e) {
					g().errorLog.add(e);
				}
			}
		}, "discover peers info on disk").start();

		new Thread(() -> {
			while (true) {
				try {
					for (var p : peers.elements) {
						if (!p.isConnected() && p.address != null) { // if not connexion
							try {
								p.setSocket(new Socket(p.address, p.port));
							} catch (IOException e) {
								p.disconnect();
								g().errorLog.add(e);
							}
						}
					}

					Thread.sleep(1012);
				} catch (InterruptedException e) {
					g().errorLog.add(e);
				}
			}
		}, "connect to peers").start();

	}

	@Override
	protected synchronized void handle(Message msg) {
		++nbMessagesReceived;
		updateInOutInfo();

		var from = findPeer(msg.route.getLast());

		if (from.publicKey != null) {
			msg.data = RSA.decrypt(msg.data, keyPair.getPrivate());
		}

		var received = serializer.fromBytes(msg.data);

		if (received instanceof Ack ack) {
			g().eventList.findEvent(ack.id).markReceivedBy(from);
		} else if (received instanceof Event e) {
			var alreadyKnownEvent = g().eventList.findEvent(e.id());

			if (alreadyKnownEvent != null) {
				alreadyKnownEvent.markReceivedBy(from);
			} else {
				g().eventList.add(e);
				e.markReceivedBy(from);
			}

			try {
				send(new Ack(e.id()));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} else if (received instanceof PeerTelemetry t) {
			if (from != null) {
				from.TokensPerSecond = t.tokensPerSecond;
				from.IsComputing = t.isComputing;
				from.promptLag = t.promptLag;
				from.queueSize = t.queueSize;
				if (t.alpha > 0)
					from.alpha = t.alpha;
			}
		} else {
			throw new IllegalStateException("received " + received.getClass());
		}
	}

	public PeerNode findPeer(InetAddress address) {
		for (var p : peers.get()) {
			if (p.address.equals(address)) {
				return p;
			}
		}

		return null;
	}

	private PeerNode findPeer(String name) {
		for (var p : peers.get()) {
			if (p.name.equals(name)) {
				return p;
			}
		}

		return null;
	}

	public PeerNode findPeer(int id) {
		for (var p : g().networkAgent.peers.get()) {
			if (p.id == id) {
				return p;
			}
		}

		return null;
	}

	public synchronized void send(Object o, PeerNode to) throws IOException {
		var msg = new Message();
		msg.route.add(peerName);
		msg.data = serializer.toBytes(o);
		msg.data = RSA.encrypt(msg.data, to.publicKey);
		var msgBytes = GZip.gzip(serializer.toBytes(msg));
		tcpDriver.send(msgBytes, to);
		++packetSent;
		updateInOutInfo();
	}

	private void updateInOutInfo() {
		inOutInfo.set(nbMessagesReceived + " received, " + packetSent + " sent");
	}

	public synchronized void send(Object o) throws IOException {
		for (var to : peers.get()) {
			send(o, to);
		}
	}

	@Override
	public String whatIsThis() {
		return "network agent";
	}

	@Override
	public String toString() {
		return "received: " + nbMessagesReceived + ", sent: " + packetSent;
	}

	public java.security.PublicKey getPublicKey() {
		return this.keyPair != null ? this.keyPair.getPublic() : null;
	}
}
