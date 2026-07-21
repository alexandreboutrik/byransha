package byransha.network;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;

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
								var compressed = is.readNBytes(len);
								var uncompressed = GZip.gunzip(compressed);
								var msg = (Message) g.serializer.fromBytes(uncompressed);
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
		
		new Thread(() -> {
			try {
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
								var compressed = is.readNBytes(len);
								var uncompressed = GZip.gunzip(compressed);
								var msg = (Message) g.serializer.fromBytes(uncompressed);
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
		}, "network agent discovery thread").start();
	}

	@Override
	protected String protocol() {
		return "TCP";
	}

	@Override
	public void send(byte[] msgBytes, PeerNode to) throws IOException {
		var compressed = GZip.gzip(msgBytes);
		to.out.writeInt(compressed.length);
		to.out.write(compressed);
//		to.out.flush();
	}
}
