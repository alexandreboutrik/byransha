package byransha.ui.shell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import byransha.graph.BGraph;
import byransha.graph.BNode;
import byransha.graph.list.action.FunctionAction;
import byransha.nodes.system.ChatNode;
import byransha.nodes.system.SystemNode;
import byransha.ui.shell.Client.CommandAction;
import byransha.util.ByUtils;

public class Client extends SystemNode {
	public ChatNode currentChat;
	public static ChatNode lastActiveChat;

	@FunctionalInterface
	interface CommandAction {
		void exec(PrintWriter out, List<String> parms) throws Throwable;
	}

	record Command(String description, CommandAction action) {
	}

	private final Map<String, Command> commands = new HashMap<>();
	private Socket socket;

	public Client(Socket clientSocket, ShellServer g) throws IOException {
		super(g);
		this.socket = clientSocket;
		var in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		var out = new PrintWriter(clientSocket.getOutputStream(), true);
		var chatID = Long.valueOf(in.readLine());
		currentChat = (ChatNode) g().indexes.byId.get(chatID);
		lastActiveChat = currentChat;
		initializeCommands(g());

		new Thread(() -> {
			try {
				while (true) {
					String line = in.readLine();

					if (line == null)
						break;

					line = line.trim();

					if (line.isEmpty()) {
						out.println("Empty command received, ignoring");
					} else if (line.startsWith("auth ")) {
						var username = line.substring("auth ".length() + 1).trim();
						var userhash = username.hashCode();
						out.println(userhash);
					} else if (line.startsWith("/")) {
						execLocalCommand(line.substring(1), out);
					} else {
						execAction(line, out);
					}
				}
			} catch (Throwable err) {
				g().errorLog.add(err);
			}

			try {
				clientSocket.getOutputStream().flush();
				clientSocket.close();
			} catch (IOException e) {
			}
		}).start();
	}

	private void initializeCommands(BGraph graph) {
		commands.put("help", new Command("list available commands",
				(out, parms) -> commands.forEach((name, cmd) -> out.println(name + " - " + cmd.description))));

		commands.put("whoami", new Command("print current user", (out, parms) -> out.println(graph.getCurrentUser())));

		commands.put("pwd", new Command("print current node", (out, parms) -> out.println(currentNode())));

		commands.put("kill", new Command("kill the server's JVM", (out, parms) -> System.exit(0)));

		commands.put("actions", new Command("list actions available on this node",
				(out, parms) -> currentNode().actions().forEach(a -> out.println(a.technicalName()))));

		commands.put("ls", new Command("list outs",
				(out, parms) -> currentNode().forEachOut((name, node) -> out.println(name + ": " + node))));

		commands.put("lf", new Command("list fields", (out, parms) -> currentNode()
				.forEachOutInFields(currentNode().getClass(), BNode.class, (f, o, ro) -> out.println(f.getName()))));

		commands.put("id", new Command("print current node ID", (out, parms) -> out.println(currentNode().id())));

		commands.put("name", new Command("print current node name", (out, parms) -> out.println(currentNode())));
		commands.put("chat", new Command("print current chat ID", (out, parms) -> out.println(currentChat.id())));
		commands.put("chats", new Command("print available chats", (out, parms) -> out
				.println(currentChat.g().currentUser().chats.elements.stream().map(c -> c.idAsText()).toList())));
		commands.put("newchat",
				new Command("create new chat", (out, parms) -> out.println(new ChatNode(graph.currentUser()).id())));
		commands.put("setcurrentchat", new Command("change chat", (out, parms) -> out
				.println(lastActiveChat = currentChat = (ChatNode) graph.indexes.byId.getByText(parms.removeFirst()))));
		commands.put("deletechat",
				new Command("delete node", (out, parms) -> graph.indexes.byId.getByText(parms.removeFirst()).delete()));
	}

	private void execAction(String actionName, PrintWriter out) throws Throwable {
		var action = currentNode().findAction(actionName);

		if (action == null) {
			out.println(
					"no such action " + actionName + " on node " + currentNode() + " of " + currentNode().getClass());
		} else {
			action.chat = currentChat;
			action.execSync();

			if (action instanceof FunctionAction fa) {
				out.println(fa.result.describeAsJSON().toPrettyString());
			}

			out.println("*" + action + " completed in " + ByUtils.ms2string(action.durationMs.get()) + "ms:");
		}
	}

	private void execLocalCommand(String cmdLine, PrintWriter out) {
		var l = new ArrayList<>(List.of(cmdLine.split(" +")));
		var cmdName = l.removeFirst();
		var cmd = commands.get(cmdName);

		if (cmd != null) {
			try {
				cmd.action.exec(out, l);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		} else {
			out.println("Unknown command: " + cmdName);
		}
	}

	private BNode currentNode() {
		return currentChat.currentNode();
	}

	@Override
	public String toString() {
		return socket.getInetAddress().getHostName() + ":" + socket.getPort();
	}
}
