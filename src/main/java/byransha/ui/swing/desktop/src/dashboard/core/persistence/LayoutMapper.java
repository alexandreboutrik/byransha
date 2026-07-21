package byransha.ui.swing.desktop.src.dashboard.core.persistence;

import byransha.ui.swing.desktop.src.dashboard.core.engine.DashboardLayout;
import byransha.ui.swing.desktop.src.dashboard.core.model.LayoutNode;
import byransha.ui.swing.desktop.src.dashboard.core.model.PanelNode;
import byransha.ui.swing.desktop.src.dashboard.core.model.SplitNode;

public class LayoutMapper {

	public DashboardLayoutSave exportLayout(DashboardLayout layout) {
		DashboardLayoutSave save = new DashboardLayoutSave();
		save.root = toSaveNode(layout.getRoot());
		return save;
	}

	private NodeSave toSaveNode(LayoutNode node) {

		if (node instanceof PanelNode panel) {
			return mapPanel(panel);
		}

		if (node instanceof SplitNode split) {
			return mapSplit(split);
		}

		throw new RuntimeException("Unknown node type");
	}

	public LayoutNode fromSaveNode(NodeSave save) {

		if (save.type.equals("panel")) {
			return mapPanelSave((PanelSave) save);
		}

		if (save.type.equals("split")) {
			return mapSplitSave((SplitSave) save);
		}

		throw new RuntimeException("Unknown save node type: " + save.type);
	}

	private PanelSave mapPanel(PanelNode panel) {
		PanelSave save = new PanelSave();
		save.name = panel.getPanelName();
		return save;
	}

	private SplitSave mapSplit(SplitNode split) {
		SplitSave save = new SplitSave();
		save.horizontal = split.getOrientation();

		for (LayoutNode child : split.getChildren()) {
			save.children.add(toSaveNode(child));
		}
		return save;
	}

	private PanelNode mapPanelSave(PanelSave save) {
		return new PanelNode(save.name);
	}

	private SplitNode mapSplitSave(SplitSave save) {

		Boolean horizontal = save.horizontal;
		SplitNode split = new SplitNode(horizontal);

		for (NodeSave childSave : save.children) {
			LayoutNode childNode = fromSaveNode(childSave);
			split.addChild(childNode);
		}

		return split;
	}
}