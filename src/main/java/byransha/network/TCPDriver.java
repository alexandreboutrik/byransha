package byransha.network;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;

import byransha.security.RSA;
import byransha.util.GZip;

public class TCPDriver extends IPDriver {
	public static final int DEFAULT_PORT = 9876;
	ServerSocket socket;

	public TCPDriver(NetworkAgent g, int port) throws FileNotFoundException, IOException {
		super(g, port);

		new Thread(() -> {
			try {
				socket = new ServerSocket(port);
				System.out.println("TCP Server is listening on port " + port);

				while (true) {
					var client = socket.accept();

					new Thread(() -> {
						var from = client.getInetAddress();
						var peer = na().findPeer(from);

						try {
							peer.out = new ObjectOutputStream(client.getOutputStream());
							var is = new ObjectInputStream(client.getInputStream());

							while (true) {
								int len = is.readInt();
								var data = is.readNBytes(len);
								data = RSA.decrypt(data, g.privateKey);
								data = RSA.decrypt(data, peer.publicKey);
								var msg = (Message) g.serializer.fromBytes(data);
								g.handle(msg);
							}
						} catch (IOException err) {
							g().errorLog.add(err);
						}
					}).start();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, "network agent TCP reception thread").start();

	}

	@Override
	protected String protocol() {
		return "TCP";
	}

	@Override
	public void send(byte[] msgBytes, PeerNode to) throws IOException {
		msgBytes = RSA.encrypt(msgBytes, g().networkAgent.privateKey);
		msgBytes = RSA.encrypt(msgBytes, to.publicKey);
		to.out.writeInt(msgBytes.length);
		to.out.write(msgBytes);
		// to.out.flush();
	}
}
