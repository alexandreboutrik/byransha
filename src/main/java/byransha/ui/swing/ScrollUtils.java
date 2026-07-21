package byransha.ui.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.Timer;

public class ScrollUtils {

	/**
	 * Waits for content to appear in the scroll pane, then smoothly scrolls to the
	 * bottom.
	 *
	 * @param scrollPane
	 *                           the target JScrollPane
	 * @param maxWaitMs
	 *                           maximum time to wait in milliseconds
	 * @param pollIntervalMs
	 *                           how often to check for content (ms)
	 */
	public static void scrollToBottomWhenReady(JScrollPane scrollPane, int maxWaitMs, int pollIntervalMs) {
		long startTime = System.currentTimeMillis();

		Timer timer = new Timer(pollIntervalMs, null);
		timer.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JScrollBar vertical = scrollPane.getVerticalScrollBar();
				int maxValue = vertical.getMaximum() - vertical.getVisibleAmount();

				if (maxValue > 0) {
					// Content is ready — smooth scroll to bottom
					timer.stop();
					smoothScrollTo(vertical, maxValue, 300);
				} else if (System.currentTimeMillis() - startTime >= maxWaitMs) {
					// Timeout — give up
					timer.stop();
				}
			}
		});

		timer.start();
	}

	/**
	 * Smoothly animates the scrollbar from its current value to the target value.
	 *
	 * @param scrollBar
	 *                       the JScrollBar to animate
	 * @param target
	 *                       the target scroll value (use scrollBar.getMaximum() -
	 *                       scrollBar.getVisibleAmount() for bottom)
	 * @param durationMs
	 *                       total animation duration in milliseconds
	 */
	public static void smoothScrollTo(JScrollBar scrollBar, int target, int durationMs) {
		int start = scrollBar.getValue();
		int distance = target - start;
		int steps = durationMs / 16; // ~60fps

		if (steps == 0 || distance == 0) {
			scrollBar.setValue(target);
			return;
		}

		Timer animator = new Timer(16, null);
		int[] step = { 0 };

		animator.addActionListener(e -> {
			step[0]++;
			double progress = (double) step[0] / steps;
			double eased = easeInOut(progress); // smooth curve

			int newValue = start + (int) (distance * eased);
			scrollBar.setValue(newValue);

			if (step[0] >= steps) {
				scrollBar.setValue(target); // ensure we land exactly
				((Timer) e.getSource()).stop();
			}
		});

		animator.start();
	}

	/**
	 * Ease-in-out curve for natural-feeling animation.
	 */
	private static double easeInOut(double t) {
		return t < 0.5
				? 2 * t * t
				: -1 + (4 - 2 * t) * t;
	}
}