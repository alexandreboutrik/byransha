package byransha.ui.swing;

import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class RoundedBorder extends AbstractBorder {
	private int radius;
	private Color color;
	private int thickness;

	public RoundedBorder(Color color, int radius, int thickness) {
		this.color = color;
		this.radius = radius;
		this.thickness = thickness;
	}

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
		Graphics2D g2d = (Graphics2D) g.create();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setColor(color);
		g2d.setStroke(new BasicStroke(thickness));

		// Dessine le rectangle arrondi en tenant compte de l'épaisseur du trait
		g2d.draw(new RoundRectangle2D.Double(x + (thickness / 2.0), y + (thickness / 2.0),
				width - thickness, height - thickness, radius, radius));
		g2d.dispose();
	}

	@Override
	public Insets getBorderInsets(Component c) {
		return new Insets(this.radius / 2, this.radius / 2, this.radius / 2, this.radius / 2);
	}
}