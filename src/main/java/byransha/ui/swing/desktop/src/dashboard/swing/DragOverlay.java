package byransha.ui.swing.desktop.src.dashboard.swing;

import java.awt.*;

import javax.swing.JComponent;

public class DragOverlay extends JComponent {
	private Rectangle rect;

	public DragOverlay() {
		this.setOpaque(false);
		this.setEnabled(false);
	}

	public void showRect(Rectangle r) {
		if (r == null || r.equals(this.rect)) {
			return;
		}
		this.rect = r;
		repaint();
	}

	public void clear() {
		this.rect = null;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		if (rect != null) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setColor(new Color(0, 120, 215, 80));
			g2.fill(rect);

			g2.setColor(new Color(0, 120, 215));
			g2.setStroke(new BasicStroke(2));
			g2.draw(rect);

			g2.dispose();
		}
	}
}