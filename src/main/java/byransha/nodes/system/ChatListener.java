package byransha.nodes.system;

public interface ChatListener {
	void newChat(User user, ChatNode chat);

	void chatClosed(User user, ChatNode chat);
}
