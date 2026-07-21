package byransha.ui.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.JPanel;

public class ResizableByGrip extends JPanel {
	public ResizableByGrip(Component scrollPane) {
		setLayout(new BorderLayout());
		add(scrollPane, BorderLayout.CENTER);

		JPanel grip = new JPanel() {
			private int startY;
			private int startHeight;

			{
				setPreferredSize(new Dimension(0, 8));
				setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));

				addMouseListener(new MouseAdapter() {
					@Override
					public void mousePressed(MouseEvent e) {
						startY = e.getYOnScreen();
						startHeight = scrollPane.getHeight();
					}
				});

				addMouseMotionListener(new MouseMotionAdapter() {
					@Override
					public void mouseDragged(MouseEvent e) {
						int delta = e.getYOnScreen() - startY;
						int newHeight = Math.max(50, startHeight + delta);
						scrollPane.setPreferredSize(new Dimension(scrollPane.getWidth(), newHeight));
						ResizableByGrip.this.revalidate();
					}
				});
			}

			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				// Draw a subtle grip indicator
				int cx = getWidth() / 2;
				int cy = getHeight() / 2;
				g.setColor(Color.GRAY);
				for (int i = -12; i <= 12; i += 6) {
					g.fillOval(cx + i, cy - 1, 3, 3);
				}
			}
		};

		add(grip, BorderLayout.SOUTH);
	}
}
