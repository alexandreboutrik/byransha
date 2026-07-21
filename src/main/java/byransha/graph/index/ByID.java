package byransha.graph.index;

import java.util.Objects;
import java.util.Random;

import byransha.graph.BNode;
import byransha.graph.Index;
import byransha.util.Base62;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public class ByID extends Index {
	protected ByID(AllIndexes allIndexes) {
		super(null);
		// super(allIndexes);
	}

	private final Long2ObjectMap<BNode> m = new Long2ObjectOpenHashMap<>();
	final Random r = new Random();

	public synchronized long forceIndex(BNode n, long newID) {
		Objects.requireNonNull(n);

		if (m.containsKey(newID)) {
			throw new IllegalStateException(newID + " is already used by node " + m.get(newID));
		}

		if (m.remove(n.id()) != n)
			throw new IllegalStateException();

		n.id = newID;
		m.put(newID, n);
		return newID;
	}

	private synchronized void assignUniqueIndexTo(final BNode n) {
		if (n.id != -1)
			throw new IllegalStateException();

		n.id = r.nextLong();
		while (m.containsKey(++n.id))
			;
		m.put(n.id, n);
	}

	@Override
	public void add(BNode n) {
		assignUniqueIndexTo(n);
	}

	@Override
	public void delete(BNode n) {
		long id = n.id();
		m.remove(id);
	}

	@Override
	public String strategy() {
		return "ID";
	}

	public BNode get(long id) {
		return m.get(id);
	}

	public BNode getByText(String id) {
		return m.get(Base62.decode(id));
	}
}