package byransha.event;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import byransha.graph.BNode;

public class InMemoryEventList extends EventList {
	List<Event> q = new ArrayList<>();
	int lastExecutedEventIndex = -1;

	public InMemoryEventList(BNode g) {
		super(g);
	}

	@Override
	public String toString() {
		return whatIsThis();
	}

	@Override
	public void add(Event e) {
		int index = Collections.binarySearch(q, e);

		if (index > 0)
			throw new IllegalStateException("event with id " + e.id() + " already exists");

		int insertionPoint = -(index + 1);
		q.add(insertionPoint, e);
	}

	@Override
	public Event remove(long id) throws IOException {
		int index = indexedBinarySearch(q, id);
		return q.remove(index);
	}

	private static <T> int indexedBinarySearch(List<Event> l, long id) {
		int low = 0;
		int high = l.size() - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			var midVal = l.get(mid);
			int cmp = Long.compare(midVal.id(), id);

			if (cmp < 0)
				low = mid + 1;
			else if (cmp > 0)
				high = mid - 1;
			else
				return mid; // key found
		}
		return -(low + 1); // key not found
	}

	@Override
	public Event forward() throws Throwable {
		if (lastExecutedEventIndex < q.size() - 1) {
			var e = q.get(++lastExecutedEventIndex);
			e.apply(g());
			currentDate = e.date;
			return e;
		} else {
			return null;
		}
	}

	@Override
	public Event rewind() throws Throwable {
		if (lastExecutedEventIndex >= 0) {
			var e = q.get(lastExecutedEventIndex--);
			e.undo(g());
			currentDate = e.date;
			return e;
		} else {
			return null;
		}
	}

	@Override
	public String whatIsThis() {
		return "an event-list in RAM";
	}

	@Override
	protected void forEachEvent(Consumer<Event> c) {
		q.forEach(c);
	}

	@Override
	public Event findEvent(long eventID) {
		for (var e : q) {
			if (e.id() == eventID)
				return e;
		}

		return null;
	}
}
