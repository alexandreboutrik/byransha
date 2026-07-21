package byransha.ui.swing.desktop.src.dashboard.core.model;

public abstract class LayoutNode {
	protected String id;
	protected SplitNode parent;

	public String getId() {
		return id;
	}

	public SplitNode getParent() {
		return parent;
	}

	public void setParent(SplitNode parent) {
		this.parent = parent;
	}

	public void removeParent() {
		this.parent = null;
	}
}