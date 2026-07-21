package byransha.ui.swing;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.List;

import javax.swing.JFrame;

public class WindowGridManager {

	public static void arrangeInGrid(List<JFrame> frames) {
		if (frames.isEmpty())
			return;

		// 1. Get screen dimensions (excluding Taskbar)
		GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
				.getDefaultConfiguration();
		Rectangle screenBounds = gc.getBounds();
		Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gc);

		int availableWidth = screenBounds.width - screenInsets.left - screenInsets.right;
		int availableHeight = screenBounds.height - screenInsets.top - screenInsets.bottom;

		// 2. Determine grid size
		int n = frames.size();
		int cols = (int) Math.ceil(Math.sqrt(n));
		int rows = (int) Math.ceil((double) n / cols);

		// 3. Calculate individual window size
		int frameW = availableWidth / cols;
		int frameH = availableHeight / rows;

		// 4. Position each frame
		for (int i = 0; i < n; i++) {
			int col = i % cols;
			int row = i / cols;

			int x = screenInsets.left + (col * frameW);
			int y = screenInsets.top + (row * frameH);

			JFrame f = frames.get(i);
			f.setSize(frameW, frameH);
			f.setLocation(x, y);
			f.setVisible(true);
		}
	}
}