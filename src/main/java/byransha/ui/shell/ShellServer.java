package byransha.ui.shell;

import java.io.IOException;
import java.net.ServerSocket;

import byransha.graph.BGraph;
import byransha.nodes.system.SystemNode;

public class ShellServer extends SystemNode {

	public static final int DEFAULT_PORT = 42424;

	public ShellServer(BGraph g, int port) throws Throwable {
		super(g);
		System.out.println("Starting shell server on port " + port);

		new Thread(() -> {
			try {
				startServer(port);
			} catch (Throwable err) {
				g().errorLog.add(err);
			}
		}).start();
	}

	private void startServer(int port) throws Throwable {
		try (var serverSocket = new ServerSocket(port)) {
			System.out.println("Telnet server listening on port " + port);

			while (true) {
				try {
					var clientSocket = serverSocket.accept();
					Client client = new Client(clientSocket, this);

				} catch (IOException e) {
					System.err.println("Client error: " + e.getMessage());
				}
			}
		}
	}

	@Override
	public String whatIsThis() {
		return "the TCP server for command line interaction with the graph";
	}

	@Override
	public String toString() {
		return "shell server";
	}
}