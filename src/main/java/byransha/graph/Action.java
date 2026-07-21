package byransha.graph;

import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import com.fasterxml.jackson.databind.node.ObjectNode;

import byransha.nodes.primitive.LongNode;
import byransha.nodes.system.ChatNode;
import byransha.nodes.system.User;
import byransha.ui.swing.ChatSheet;

public abstract class Action<HOOK extends BNode> extends BNode {
	public boolean stopRequested = false;
	private Thread thread;
	public final Class<? extends Category>[] path;
	public final LongNode durationMs = new LongNode(this);
	public ChatNode chat;
	public Consumer<Object> outputConsumer;
	public Consumer<Double> progressConsumer;
	public JProgressBar progressBar;
	public boolean confirmationRequired = false;
	public boolean hasButtonOnKishanView = false;

	public Action(HOOK parent, Class<? extends Category>... pathInMenu) {
		super(parent);
		this.path = pathInMenu;
	}

	public List<BNode> parameters() {
		var r = new ArrayList<BNode>();
		forEachOutInFields(getClass(), Action.class, (field, out, readOnly) -> {
			if (field.isAnnotationPresent(ShowInKishanView.class)) {
				r.add(out);
			}
		});
		return r;
	}

	@ActionMethod
	public void run() {
		execSync();
	}

	@Override
	public void createActions() {
		cachedActions.elements.add(new stop(this));
		super.createActions();
	}

	@Override
	public ObjectNode describeAsJSON() {
		var r = (ObjectNode) super.describeAsJSON();
		r.put("canExecute", canExecute(g().getCurrentUser()));
		r.put("whatItDoes", whatItDoes());
		return r;
	}

	public boolean canExecute(User user) {
		return true;
	}

	public boolean wantToBeProposedFor(BNode bNode) {
		return true;
	}

	@Override
	public String toString() {
		return whatItDoes();
	}

	public String technicalName() {
		return toString().replace(' ', '_').toLowerCase();
	}

	public abstract String whatItDoes();

	public final synchronized void execAsync() {
		final var startDateMs = System.currentTimeMillis();

		this.thread = new Thread(() -> {
			try {
				impl();
				this.durationMs.set(System.currentTimeMillis() - startDateMs);
			} catch (Throwable err) {
				g().errorLog.add(err);
			}
		});

		this.thread.start();
	}

	protected abstract void impl() throws Throwable;

	protected void handleIAResponseChunk(Object chunk) {
		if (outputConsumer != null) {
			outputConsumer.accept(chunk);
		}
	}

	public abstract boolean applies();

	@Override
	public final String whatIsThis() {
		return "an action which " + whatItDoes();
	}

	public class action extends Category {
	}

	public boolean hasAlreadyBeenStarted() {
		return thread != null;
	}

	public boolean isRunning() {
		return thread != null && thread.isAlive();
	}

	public void waitForCompletion() {
		if (thread != null) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	public final void execSync() {
		execAsync();
		waitForCompletion();
	}

	public JProgressBar getProgressBar() {
		if (progressBar == null) {
			progressBar = new JProgressBar(0, 100);
			progressBar.setStringPainted(true);
			progressBar.setToolTipText(toString());

			progressConsumer = v -> {
				progressBar.setValue((int) (v * 100));
				progressBar.setString(String.format("%.2f%%", v * 100));
			};
		}
		return progressBar;
	}

	@Override
	public JComponent getListItemComponent(ChatNode chat) {
		var c = super.getListItemComponent(chat);

		if (isRunning()) {
			var p = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
			p.add(getProgressBar());
			p.add(c);
			return p;
		} else {
			return c;
		}
	}

	public boolean hasButtonOnKishanView() {
		return hasButtonOnKishanView;
	}

}
