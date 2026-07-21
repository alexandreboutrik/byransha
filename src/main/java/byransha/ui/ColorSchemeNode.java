package byransha.ui;

import java.awt.Color;

import byransha.graph.BNode;
import byransha.ui.swing.ColorPalette;
import byransha.ui.swing.ColorPalette.Style;

public class ColorSchemeNode extends BNode {

	public final Style style;

	public ColorSchemeNode(BNode parent, Style style2) {
		super(parent);
		this.style = style2;
	}

	@Override
	public String whatIsThis() {
		return "a color scheme";
	}

	@Override
	public String toString() {
		return style.name();
	}

	public Color[] get() {
		return ColorPalette.forStyle(style);
	}

}