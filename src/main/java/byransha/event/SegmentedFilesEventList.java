package byransha.event;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

import byransha.graph.BGraph;

public abstract class SegmentedFilesEventList extends EventList {
	private final File directory;

	static class Segment extends SingleFileEventList {
		public Segment(SegmentedFilesEventList parent, File f) {
			super(parent, f);
		}
	}

	private Segment currentSegment;

	public SegmentedFilesEventList(BGraph g, File directory)
			throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		super(g);
		this.directory = directory;
		this.directory.mkdirs();
	}

	@Override
	public String toString() {
		return "path: " + directory.getAbsolutePath();
	}

	@Override
	public String whatIsThis() {
		return "an event list segmented in multiple files";
	}

	@Override
	public void add(Event e) {
		var f = getFileStoring(e.id());

		if (f == currentSegment.f) {
			currentSegment.add(e);
		} else {
			var s = new Segment(this, f);
			s.add(e);
		}
	}

	public abstract File getFileStoring(long id);

	@Override
	protected void forEachEvent(Consumer<Event> c) {
		forEachFile(f -> {
			if (f == currentSegment.f) {
				currentSegment.forEachEvent(c);
			} else {
				new Segment(this, f).forEachEvent(c);
			}
		});
	}

	protected abstract void forEachFile(Consumer<File> c);

	@Override
	public Event forward() throws Throwable {
		if (currentSegment.forward() == null) {
			currentSegment = new Segment(this, nextSegment(currentSegment.f));

			if (currentSegment == null) {
				return null;
			}

			currentSegment.forward();
		}

		return null;
	}

	protected abstract File nextSegment(File f);

	protected abstract File previousSegment(File f);

	@Override
	public Event rewind() throws Throwable {
		if (currentSegment.rewind() == null) {
			currentSegment = new Segment(this, previousSegment(currentSegment.f));

			if (currentSegment == null) {
				return null;
			}

			currentSegment.forward();
		}
		return null;
	}

	@Override
	public Event findEvent(long eventID) {
		var f = getFileStoring(eventID);

		if (f == currentSegment.f) {
			return currentSegment.findEvent(eventID);
		} else {
			return new Segment(this, f).findEvent(eventID);
		}
	}

	@Override
	public Event remove(long eventID) throws IOException {
		var f = getFileStoring(eventID);

		if (f == currentSegment.f) {
			return currentSegment.remove(eventID);
		} else {
			return new Segment(this, f).remove(eventID);
		}
	}

}
