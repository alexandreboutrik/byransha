package byransha.ui.swing;

import java.awt.Color;

import javax.swing.JLabel;

import byransha.graph.BNode;
import byransha.graph.list.action.ListNode;
import byransha.nodes.primitive.ValueChangeListener;
import byransha.nodes.primitive.ValuedNode;
import byransha.util.ListenableList;

public class ErrorIndicator extends JLabel {
	final BNode n;

	public ErrorIndicator(BNode n) {
		this.n = n;
		setForeground(Color.red);
		setOpaque(false);
		update();

		if (n instanceof ValuedNode vn) {
			vn.valueChangeListeners.add(new ValueChangeListener() {

				@Override
				public void changed(ValuedNode n, Object formerValue, Object newValue) {
					update();
				}
			});
		} else if (n instanceof ListNode l) {
			l.elements.addListener(new ListenableList.Listener() {

				@Override
				public void onAdded(int index, Object element) {
					update();
				}

				@Override
				public void onRemoved(int index, Object oldElement) {
					update();
				}

				@Override
				public void onSet(int index, Object oldElement, Object newElement) {
					update();
				}
			});
		}
	}

	private void update() {
		var errors = n.errors();
		setText(errors.isEmpty() ? "" : errors.size() + "");
		setToolTipText("<html>" + n.errors().size() + " error(s): <br><ul>");
		n.errors().forEach(err -> setToolTipText(getToolTipText() + "<li>" + err.toString()));
		setToolTipText(getToolTipText() + "</ul></html>");
	}
}
