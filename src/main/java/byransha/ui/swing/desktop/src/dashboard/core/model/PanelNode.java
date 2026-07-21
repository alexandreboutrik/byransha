package byransha.ui.swing.desktop.src.dashboard.core.model;

import java.util.UUID;

public class PanelNode extends LayoutNode {

	private String panelName;

	public PanelNode(String panelName) {
		this.id = UUID.randomUUID().toString();
		this.panelName = panelName;
	}

	public String getPanelName() {
		return panelName;
	}

	@Override
	public String toString() {
		return "Panel(" + panelName + ")";
	}
}