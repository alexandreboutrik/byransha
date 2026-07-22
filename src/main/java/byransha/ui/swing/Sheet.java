package byransha.ui.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.Scrollable;

import byransha.graph.BNode;
import byransha.translate.Translator;

public class Sheet extends JPanel implements Scrollable {
	public WrapPanel currentLine = newLine();
	Color bgColor;

	// private final ChatNode chat;
	// int colorIndex;
	// Color[] backgroundColors = new Color[] { Color.white, new Color(0xEE, 0xEE,
	// 0xEE, 10) };

	public Sheet() {
		super();
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setOpaque(false);
		add(currentLine);
	}

	@Override
	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		return 20;
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
		return 60;
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
		return true; // match viewport width so WrapPanels reflow correctly
	}

	@Override
	public boolean getScrollableTracksViewportHeight() {
		return false; // allow vertical scrolling
	}

	public TextDisplayComponent appendToCurrentLine(String s, Translator translator) {
		var t = new TextDisplayComponent(translator, s);
		appendToCurrentLine(t);
		return t;
	}

	public void appendToCurrentLine(JComponent c) {
		currentLine.add(c);
	}

	public WrapPanel newLine() {
		currentLine = new WrapPanel();
		currentLine.setOpaque(false);
		currentLine.setBackground(bgColor);
		add(currentLine);
		return currentLine;
	}

	public void clear() {
		removeAll();
		revalidate();
		repaint();
	}

	public void end() {
		addSeparator();
		add(Box.createVerticalGlue());
	}

	public void addSeparator() {
		var separator = new JSeparator();
		separator.setPreferredSize(new Dimension(Integer.MAX_VALUE, 1));
		newLine();
		newLine();
		add(separator);
	}

	public void removeNode(BNode n) {
		// TODO
	}

}
