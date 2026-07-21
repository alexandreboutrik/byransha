package byransha.ui.swing;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

public class SelectableTooltip {

	public static void addSelectableTooltip(JComponent owner, String htmlContent) {
		// We use a JWindow to act as the tooltip
		JWindow window = new JWindow();

		// Use a JEditorPane to render HTML and allow selection
		JEditorPane editorPane = new JEditorPane("text/html", htmlContent);
		editorPane.setEditable(false);
		editorPane.setBackground(new Color(255, 255, 225)); // Classic tooltip yellow
		editorPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));

		window.add(new JScrollPane(editorPane));
		window.pack();

		owner.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				// Position the window near the mouse
				Point p = owner.getLocationOnScreen();
				window.setLocation(p.x + e.getX(), p.y + e.getY() + 20);
				window.setVisible(true);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				// Important: Only hide if the mouse didn't move INTO the tooltip window
				SwingUtilities.invokeLater(() -> {
					PointerInfo pi = MouseInfo.getPointerInfo();
					if (pi != null) {
						Point mousePos = pi.getLocation();
						if (!window.getBounds().contains(mousePos)) {
							window.setVisible(false);
						}
					}
				});
			}
		});

		// Ensure the window closes if the mouse leaves the window itself
		editorPane.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseExited(MouseEvent e) {
				PointerInfo pi = MouseInfo.getPointerInfo();
				if (pi != null && !owner.getBounds().contains(pi.getLocation())) {
					window.setVisible(false);
				}
			}
		});
	}

	public static void main(String[] args) {
		JFrame frame = new JFrame("Byransha Debugger");
		JButton btn = new JButton("Hover for Node Data");

		addSelectableTooltip(btn, "<html><b>Node ID:</b> 0x8823<br><b>Key:</b> CopyMe12345</html>");

		frame.setLayout(new FlowLayout());
		frame.add(btn);
		frame.setSize(300, 200);
		frame.setVisible(true);
	}
}