package byransha.ui.swing.desktop.src.dashboard.swing;

import byransha.ui.swing.desktop.src.dashboard.core.model.Direction;
import byransha.ui.swing.desktop.src.dashboard.core.model.PanelNode;
import byransha.ui.swing.desktop.src.dashboard.core.model.SplitNode;

public interface DashboardActions {
	void refreshUI();

	void splitPanel(PanelNode panel, Boolean horizontal);

	void closePanel(PanelNode panel);

	void movePanel(PanelNode panel, PanelNode desinationPanel, Direction direction);

	void rotateSplit(SplitNode split);

	void setDividerLocation(SplitNode split, int location);
}