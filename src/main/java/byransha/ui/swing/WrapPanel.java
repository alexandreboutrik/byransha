package byransha.ui.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

import javax.swing.JPanel;

// A FlowLayout panel that correctly reports its preferred height
// based on its current width, so BoxLayout can allocate the right space
public class WrapPanel extends JPanel {
	public WrapPanel() {
		super(new FlowLayout(FlowLayout.LEFT, 10, 5));

	}

	@Override
	public Dimension getPreferredSize() {
		int width = getSize().width;
		if (width == 0)
			width = super.getPreferredSize().width;

		FlowLayout fl = (FlowLayout) getLayout();
		int hgap = fl.getHgap();
		int vgap = fl.getVgap();
		Insets insets = getInsets();

		// Subtract all border insets from available width
		int maxWidth = width - insets.left - insets.right - hgap * 2;

		int rowWidth = 0;
		int rowHeight = 0;
		int totalHeight = insets.top + insets.bottom + vgap;
		boolean firstInRow = true;

		for (Component c : getComponents()) {
			Dimension d = c.getPreferredSize();
			int needed = firstInRow ? d.width : hgap + d.width;
			if (!firstInRow && rowWidth + needed > maxWidth) {
				totalHeight += rowHeight + vgap;
				rowWidth = 0;
				rowHeight = 0;
				firstInRow = true;
				needed = d.width;
			}
			rowWidth += needed;
			rowHeight = Math.max(rowHeight, d.height);
			firstInRow = false;
		}
		totalHeight += rowHeight + vgap;

		return new Dimension(width, totalHeight);
	}

	@Override
	public Dimension getMaximumSize() {
		return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
	}
}