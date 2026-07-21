package byransha.ui.swing.desktop.src.dashboard.swing;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import byransha.ui.swing.desktop.src.dashboard.core.model.Direction;
import byransha.ui.swing.desktop.src.dashboard.core.model.PanelNode;

public class PanelDragManager {
	private PanelNode draggedPanel;
	private PanelNode targetPanel;
	private PanelNode prevTarget;
	private Direction prevDirection;
	private Direction direction;

	public void makeDraggable(JComponent header, PanelNode panel, DashboardActions actions, DragOverlay overlay) {
		header.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e)) {
					draggedPanel = panel;
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e)) {
					if (draggedPanel != null && targetPanel != null && direction != null) {
						actions.movePanel(draggedPanel, targetPanel, direction);
					}
					draggedPanel = null;
					targetPanel = null;
					direction = null;
					overlay.clear();
				}
			}
		});

		header.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e)) {
					Point p = SwingUtilities.convertPoint(header, e.getPoint(), header.getRootPane());
					Component comp = SwingUtilities.getDeepestComponentAt(header.getRootPane().getContentPane(), p.x,
							p.y);

					PanelMatch match = findPanelMatch(comp);
					if (match != null) {
						PanelNode found = match.node;
						JComponent panelComp = match.component;
						if (found != draggedPanel) {
							targetPanel = found;
							direction = computeDirection(comp, p);

							Rectangle bounds = SwingUtilities.convertRectangle(panelComp.getParent(),
									panelComp.getBounds(), header.getRootPane());

							if (prevDirection != direction || prevTarget != targetPanel) {
								prevDirection = direction;
								prevTarget = targetPanel;
								if (direction != null) {
									Rectangle drawRect = computeSplitRectangle(bounds, direction);
									overlay.showRect(drawRect);
								} else {
									overlay.clear();
								}
							}

						} else {
							direction = null;
							overlay.clear();
						}
					}
				}
			}
		});
	}

	private record PanelMatch(PanelNode node, JComponent component) {
	}

	private PanelMatch findPanelMatch(Component comp) {
		while (comp != null) {
			if (comp instanceof JComponent jc) {
				Object obj = jc.getClientProperty("panelNode");
				if (obj instanceof PanelNode pn) {
					return new PanelMatch(pn, jc);
				}
			}
			comp = comp.getParent();
		}
		return null;
	}

	private Rectangle computeSplitRectangle(Rectangle bounds, Direction dir) {
		int w = bounds.width;
		int h = bounds.height;

		switch (dir) {
			case LEFT:
				return new Rectangle(bounds.x, bounds.y, w / 2, h);
			case RIGHT:
				return new Rectangle(bounds.x + w / 2, bounds.y, w / 2, h);
			case TOP:
				return new Rectangle(bounds.x, bounds.y, w, h / 2);
			case BOTTOM:
				return new Rectangle(bounds.x, bounds.y + h / 2, w, h / 2);
			default:
				return bounds;
		}
	}

	private Direction computeDirection(Component target, Point screenPoint) {
		Point loc = target.getLocationOnScreen();

		int x = screenPoint.x - loc.x;
		int y = screenPoint.y - loc.y;

		int w = target.getWidth();
		int h = target.getHeight();

		if (x < w * 0.25)
			return Direction.LEFT;
		if (x > w * 0.75)
			return Direction.RIGHT;
		if (y < h * 0.25)
			return Direction.TOP;
		if (y > h * 0.75)
			return Direction.BOTTOM;

		return null;
	}
}
