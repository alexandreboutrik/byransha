package byransha.nodes.system;

import java.awt.GridLayout;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import byransha.graph.BGraph;
import byransha.graph.BNode;
import byransha.translate.Translator;
import byransha.ui.swing.TranslatableTextArea;

public class ConsoleNode extends BNode {
	private final Set<C> stdOutComponents = new HashSet<>();
	private final Set<C> stdErrComponents = new HashSet<>();

	public static class C extends TranslatableTextArea {

		public C(Translator translator) {
			super(translator);
			setEditable(false);
		}

		public void append(String text) {
			SwingUtilities.invokeLater(() -> {
				append(text);
				setCaretPosition(getDocument().getLength());
			});
		}
	}

	public static class ToQOutputStream extends OutputStream {
		final Set<C> components;
		private final PrintStream originalStream;

		ToQOutputStream(Set<C> components, PrintStream originalStream) {
			this.components = components;
			this.originalStream = originalStream;
		}

		@Override
		public void write(int b) {
			write(new byte[] { (byte) b }, 0, 1);
		}

		@Override
		public void write(byte[] b, int off, int len) {
			String text = new String(b, off, len);
			originalStream.print(text);
			components.forEach(c -> c.append(text));
		}
	}

	public ConsoleNode(BGraph g) {
		super(g);
		System.setOut(new PrintStream(new ToQOutputStream(stdOutComponents, System.out), true));
		System.setErr(new PrintStream(new ToQOutputStream(stdErrComponents, System.err), true));
	}

	@Override
	protected JComponent getSmallComponent(ChatNode chat) {
		var p = new JPanel(new GridLayout(2, 1));
		var stdoutC = new C(g().translator);
		stdOutComponents.add(stdoutC);
		p.add(stdoutC);
		var stderrC = new C(g().translator);
		stdOutComponents.add(stderrC);
		p.add(stderrC);
		return p;
	}

}
