package byransha.network;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import byransha.util.GZip;

public class UDPDriver extends IPDriver {
	DatagramSocket socket;

	public UDPDriver(NetworkAgent g, int port) throws FileNotFoundException, IOException {
		super(g, port);

		new Thread(() -> {
			try {
				socket = new DatagramSocket(port);
				System.out.println("UDP Server is listening on port " + port);

				byte[] receiveBuffer = new byte[1024];

				while (true) {
					DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
					socket.receive(packet);
					var msg = (Message) g.serializer.fromBytes(GZip.gunzip(packet.getData()));
					++packetReceived;
					updateInOutInfo();
					g.handle(msg);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, protocol() + " reception thread").start();
	}

	@Override
	public void send(byte[] msgBytes, PeerNode to) throws IOException {
		var sendPacket = new DatagramPacket(msgBytes, msgBytes.length, to.address, to.port);
		socket.send(sendPacket);
	}

	@Override
	protected String protocol() {
		return "UDP";
	}
}
