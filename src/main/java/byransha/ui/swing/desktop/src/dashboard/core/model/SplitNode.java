package byransha.ui.swing.desktop.src.dashboard.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SplitNode extends LayoutNode {

	private Boolean horizontal;
	private List<LayoutNode> children = new ArrayList<>();
	private int dividerLocation;

	public SplitNode(Boolean horizontal) {
		this.id = UUID.randomUUID().toString();
		this.horizontal = horizontal;
	}

	public Boolean getOrientation() {
		return horizontal;
	}

	public void switchOrientation() {
		horizontal = !horizontal;
	}

	public List<LayoutNode> getChildren() {
		return children;
	}

	public LayoutNode getChild(int index) {
		return children.get(index);
	}

	public void addChild(LayoutNode node) {
		children.add(node);
		node.setParent(this);
	}

	public void removeChild(LayoutNode node) {
		children.remove(node);
		node.removeParent();
	}

	public void replaceChild(int index, LayoutNode newChild) {
		children.set(index, newChild);
		newChild.setParent(this);
	}

	public int getDividerLocation() {
		return dividerLocation;
	}

	public void setDividerLocation(int location) {
		dividerLocation = location;
	}

	@Override
	public String toString() {
		return "Split(" + (horizontal ? "HORIZONTAL" : "VERTICAL") + ")";
	}
}