package byransha.nodes.primitive;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.Files;

import byransha.graph.BNode;
import byransha.graph.NodeError;
import byransha.nodes.system.Byransha;

public abstract class ValuedNode<V> extends BNode {
	V value;
	boolean valueRequired;
	public final List<ValueChangeListener<V>> valueChangeListeners = new ArrayList<>();
	private boolean shownOnDisk;

	public ValuedNode(BNode parent) {
		super(parent);
		shownOnDisk = enclosingBusinessNode() == null; // all technical info is printed on disk
	}

	@Override
	public ObjectNode describeAsJSON() {
		var r = super.describeAsJSON();
		r.put("value", toString());
		return r;
	}

	@Override
	protected void fillErrors(List<NodeError> errs) {
		if (valueRequired && value == null) {
			errs.add(new NodeError(this, "a value is required"));
		}
	}

	public V get() {
		if (false)// !canSee(g().currentUser()))
			throw new RuntimeException(g().currentUser() + " is not allowed to read the value");

		return value;
	}

	public V getOrDefault(V defaultValue) {
		var value = get();

		if (value == null) {
			return defaultValue;
		}

		return value;
	}

	public void reset() {
		set(defaultValue());
	}

	public void set_checkPermissions(V newValue) {
		if (g() != null && !canEdit(g().currentUser()))
			throw new RuntimeException(g().currentUser() + " is not allowed to set value");

		set(newValue);
	}

	public void set(V newValue) {
		if (readOnly)
			throw new RuntimeException("can't change a read only valued node");

		V oldValue = value;
		boolean valueChange = newValue != value || (value != null && !value.equals(newValue));

		value = newValue;

		if (valueChange) {
			valueChangeListeners.forEach(l -> l.changed(this, oldValue, newValue));
		}

		if (shouldGenerateEvent()) {
			var g = g();
			if (g.eventList != null) {
				// g.eventList.add(new ValuedNodeValueChangeEvent<V>(g, LocalDateTime.now(),
				// this, oldValue, newValue));
			}
		}

		if (shownOnDisk) {
			// writeValueToDisk();
		}
	}

	private void writeValueToDisk() {
		try {
			var s = toString();
			var f = new File(Byransha.homeDirectory, "valued_nodes/" + rolePath() + ".txt");

			if (s != null) {
				f.getParentFile().mkdirs();
				Files.write(s.getBytes(), f);
			} else if (f.exists()) {
				f.delete();
			}
		} catch (IOException ioError) {
			g().errorLog.add(ioError);
		}
	}

	private boolean shouldGenerateEvent() {
		return enclosingBusinessNode() != null;
	}

	public abstract V defaultValue();

	@Override
	public String toString() {
		var v = get();
		return v == null ? "null" : v.toString();
	}

	protected abstract void writeValue(V v, ObjectOutput out) throws IOException;

	protected abstract V readValue(ObjectInput in) throws IOException;
}
