package byransha.ui.swing.desktop.src.dashboard.core.engine;

import byransha.ui.swing.desktop.src.dashboard.core.model.*;

public class DashboardLayout {

	private LayoutNode root;

	public DashboardLayout() {
		root = new PanelNode("default");
	}

	public LayoutNode getRoot() {
		return root;
	}

	public void setRoot(LayoutNode root) {
		this.root = root;
	}

	public LayoutNode findNode(String id) {
		return findNodeRecursive(root, id);
	}

	public PanelNode findPanel(String id) {
		LayoutNode node = findNode(id);
		return (node instanceof PanelNode) ? (PanelNode) node : null;
	}

	public SplitNode findSplit(String id) {
		LayoutNode node = findNode(id);
		return (node instanceof SplitNode) ? (SplitNode) node : null;
	}

	public void splitPanel(PanelNode panelToSplit, Boolean horizontal) {

		PanelNode newPanel = new PanelNode("new-panel");

		SplitNode split = new SplitNode(horizontal);
		split.addChild(panelToSplit);
		split.addChild(newPanel);

		root = replaceNode(root, panelToSplit, split);
	}

	public void removePanel(PanelNode panel) {

		SplitNode parent = panel.getParent();

		if (parent == null) {
			System.out.println("Impossible de supprimer ce panel");
			return;
		}

		parent.removeChild(panel);

		// collapse si le split n'a plus qu'un enfant
		if (parent.getChildren().size() == 1) {
			LayoutNode remaining = parent.getChild(0);
			root = replaceNode(root, parent, remaining);
		}
	}

	private void insertPanel(PanelNode panelToInsert, PanelNode targetPanel, Direction direction) {

		Boolean horizontal = (direction == Direction.LEFT || direction == Direction.RIGHT) ? true : false;

		boolean insertBefore = (direction == Direction.LEFT || direction == Direction.TOP);

		SplitNode newSplit = new SplitNode(horizontal);

		if (insertBefore) {
			newSplit.addChild(panelToInsert);
			newSplit.addChild(targetPanel);
		} else {
			newSplit.addChild(targetPanel);
			newSplit.addChild(panelToInsert);
		}

		root = replaceNode(root, targetPanel, newSplit);
	}

	public void movePanel(PanelNode panel, PanelNode desinationPanel, Direction direction) {
		removePanel(panel);
		insertPanel(panel, desinationPanel, direction);
	}

	public void rotateSplit(SplitNode split) {
		split.switchOrientation();
	}

	public void setDividerLocation(SplitNode split, int location) {
		split.setDividerLocation(location);
	}

	private LayoutNode findNodeRecursive(LayoutNode node, String id) {

		if (node.getId().equals(id)) {
			return node;
		}

		if (node instanceof SplitNode split) {
			for (LayoutNode child : split.getChildren()) {
				LayoutNode result = findNodeRecursive(child, id);
				if (result != null)
					return result;
			}
		}

		return null;
	}

	private LayoutNode replaceNode(LayoutNode current, LayoutNode target, LayoutNode replacement) {

		if (current == target) {
			replacement.removeParent();
			return replacement;
		}

		if (current instanceof SplitNode split) {
			for (int i = 0; i < split.getChildren().size(); i++) {
				LayoutNode child = split.getChild(i);
				LayoutNode newChild = replaceNode(child, target, replacement);
				split.replaceChild(i, newChild);
			}
		}

		return current;
	}

}