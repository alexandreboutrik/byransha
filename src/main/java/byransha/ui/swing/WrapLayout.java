package byransha.ui.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

public class WrapLayout extends FlowLayout {

	public WrapLayout(int align, int hgap, int vgap) {
		super(align, hgap, vgap);
	}

	@Override
	public Dimension preferredLayoutSize(Container target) {
		return layoutSize(target);
	}

	@Override
	public Dimension minimumLayoutSize(Container target) {
		return layoutSize(target);
	}

	private Dimension layoutSize(Container target) {
		synchronized (target.getTreeLock()) {
			int targetWidth = target.getWidth();
			if (targetWidth == 0)
				targetWidth = Integer.MAX_VALUE;

			Insets insets = target.getInsets();
			int maxWidth = targetWidth - insets.left - insets.right;
			int hgap = getHgap();
			int vgap = getVgap();

			int rowWidth = 0, rowHeight = 0;
			int totalHeight = insets.top + insets.bottom + vgap;

			for (Component c : target.getComponents()) {
				if (!c.isVisible())
					continue;
				Dimension d = c.getPreferredSize();

				if (rowWidth > 0 && rowWidth + hgap + d.width > maxWidth) {
					totalHeight += rowHeight + vgap;
					rowWidth = 0;
					rowHeight = 0;
				}
				rowWidth += d.width + hgap;
				rowHeight = Math.max(rowHeight, d.height);
			}
			totalHeight += rowHeight;

			return new Dimension(targetWidth, totalHeight);
		}
	}
}