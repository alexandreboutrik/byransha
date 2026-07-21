package byransha.ui.swing.desktop.src.dashboard.core.persistence;

import java.util.ArrayList;
import java.util.List;

class SplitSave extends NodeSave {
	Boolean horizontal;
	List<NodeSave> children = new ArrayList<>();

	public SplitSave() {
		this.type = "split";
	};
}