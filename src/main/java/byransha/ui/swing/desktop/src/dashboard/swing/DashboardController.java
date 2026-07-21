package byransha.ui.swing.desktop.src.dashboard.swing;

import javax.swing.JComponent;
import javax.swing.JFrame;

import byransha.ui.swing.desktop.src.dashboard.core.engine.DashboardLayout;
import byransha.ui.swing.desktop.src.dashboard.core.model.Direction;
import byransha.ui.swing.desktop.src.dashboard.core.model.PanelNode;
import byransha.ui.swing.desktop.src.dashboard.core.model.SplitNode;
import byransha.ui.swing.desktop.src.dashboard.core.persistence.DashboardSaver;

public class DashboardController implements DashboardActions {
	DashboardLayout layout;
	DashboardViewBuilder builder;
	DashboardSaver saver;
	JFrame frame;
	private final DragOverlay overlay = new DragOverlay();

	public DashboardController(DashboardLayout layout, DashboardViewBuilder builder, DashboardSaver saver,
			JFrame frame) {
		this.layout = layout;
		this.builder = builder;
		this.saver = saver;
		this.frame = frame;
		frame.getRootPane().setGlassPane(overlay);
		overlay.setVisible(true);
	}

	public void refreshUI() {
		JComponent newRoot = builder.build(layout.getRoot(), this, overlay);
		frame.setContentPane(newRoot);
		frame.revalidate();
		frame.repaint();
		saver.saveToFile(layout, "src/main/java/byransha/ui/swing/desktop/save.json");
	}

	public void splitPanel(PanelNode panel, Boolean horizontal) {
		layout.splitPanel(panel, horizontal);
		refreshUI();
	}

	public void closePanel(PanelNode panel) {
		layout.removePanel(panel);
		refreshUI();
	}

	public void movePanel(PanelNode panel, PanelNode desinationPanel, Direction direction) {
		layout.movePanel(panel, desinationPanel, direction);
		refreshUI();
	}

	public void rotateSplit(SplitNode split) {
		layout.rotateSplit(split);
		refreshUI();
	}

	public void setDividerLocation(SplitNode split, int location) {
		layout.setDividerLocation(split, location);
	}
}