package byransha.nodes.primitive;

import java.awt.Color;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import byransha.graph.ActionMethod;
import byransha.graph.BNode;
import byransha.graph.NodeError;
import byransha.nodes.system.ChatNode;
import byransha.ui.swing.ChatSheet;

public class StringNode extends PrimitiveValueNode<String> {
	String re;
	public boolean hideText;

	public StringNode(BNode g) {
		this(g, null, null);
		Objects.requireNonNull(g);
	}

	public StringNode(BNode parent, String init, String re) {
		super(parent);
		this.re = re;
		if (re != null)
			Pattern.compile(re);
		set(init);
	}

	public boolean accept(String s) {
		return s != null && (re == null || re.matches(s));
	}

	@Override
	public String toString() {
		return get();
	}

	@ActionMethod
	public void reverse() {
		var s = get();
		if (s != null) {
			set(new StringBuilder(s).reverse().toString());
		}
	}

	@Override
	public String whatIsThis() {
		return "a sequence of characters";
	}

	@Override
	protected void fillErrors(List<NodeError> errs) {
		super.fillErrors(errs);
		var s = get();

		if (re != null && s != null && !s.matches(re)) {
			errs.add(new NodeError(this, "does not match " + re));
		}
	}

	@Override
	public String defaultValue() {
		return null;
	}

	@Override
	protected void writeValue(String v, ObjectOutput out) throws IOException {
		out.writeUTF(v);
	}

	@Override
	protected String readValue(ObjectInput in) throws IOException {
		return in.readUTF();
	}

	@Override
	public void writeKishanView(ChatSheet pane) {
		pane.currentLine.add(getListItemComponent(pane.chat));
	}

	@Override
	public JComponent getListItemComponent(ChatNode chat) {
		var c = getSmallComponent(chat);

		if (c.getText() != null) {
			c.setColumns(c.getText().length());
		}

		return c;
	}

	@Override
	public JTextField getSmallComponent(ChatNode chat) {
		var text = get();
		var tf = hideText ? new JPasswordField() : new JTextField();

		if (text != null) {
			tf.setText(text);
		}

		tf.setColumns(20);
		tf.setEditable(!readOnly);

		if (readOnly) {
			tf.setBorder(null);
		} else {
			tf.setBorder(new LineBorder(Color.lightGray));
		}
		ValueChangeListener<String> changeListener = (n, old, newValue) -> SwingUtilities
				.invokeLater(() -> tf.setText(newValue));
		valueChangeListeners.add(changeListener);

		tf.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {
				changed(e);
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				changed(e);
			}

			private void changed(DocumentEvent e) {
				var v = tf.getText();
				valueChangeListeners.remove(changeListener);
				set(v);
				valueChangeListeners.add(changeListener);
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
			}
		});

		return tf;
	}

}
