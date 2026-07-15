package byransha.ui.swing;

import java.awt.Color;
import java.awt.Insets;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import byransha.ai.QueryIA;
import byransha.graph.Action;
import byransha.graph.BNode;
import byransha.graph.list.action.FunctionAction;
import byransha.nodes.primitive.TextNode;
import byransha.nodes.system.ChatNode;

public class ChatSheet extends Sheet {
	public final ChatNode chat;

	public ChatSheet(ChatNode chat) {
		super();
		this.chat = chat;
		Utils.idDropTarget(chat.g(), this, n -> chat.nodes.elements.add(n));
	}

	void appendNode(BNode n) {
		this.bgColor = n.getBackgroundColor();

		newLine();
		newLine();

		var is = this;// new ChatSheet(chat);

		var path = n.path().elements;

		for (int i = 0; i < path.size(); ++i) {
			var e = path.get(i);
			is.appendToCurrentLine(e.createBall(18, 2, chat));
			is.appendToCurrentLine(e.toString());

			if (i < path.size() - 1) {
				is.appendToCurrentLine(">");
			}
		}
		
		is.currentLine.setBackground(chat.g().swing.getBackgroundColor());
		is.currentLine.setOpaque(true);

//		appendToCurrentLine(n + " (" + n.whatIsThis() + ")");
		is.newLine();
		is.newLine();
		n.writeKishanView(is);

		// appendToCurrentLine(is);
		newLine();

		revalidate();
		repaint();

		var scrollPane = Utils.getScrollPane(this);
		if (scrollPane != null) {
			ScrollUtils.scrollToBottomWhenReady(scrollPane, 2000, 100);
		}
	}

	public void appendToCurrentLine(String s) {
		super.appendToCurrentLine(s, chat.g().translator);
	}

}
