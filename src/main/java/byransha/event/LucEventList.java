package byransha.event;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

import byransha.graph.BGraph;

public class LucEventList extends SegmentedFilesEventList {
	private long threshold;
	private final File directory;

	public LucEventList(BGraph g, File directory)
			throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		super(g, directory);
		this.directory = directory;
		this.directory.mkdirs();
	}

	@Override
	public String whatIsThis() {
		return "an event list segmented by time slots";
	}

	@Override
	public void add(Event e) {
		var q = getLocalList(e);

		if (q.f.length() > threshold) {
			expand(q);
		}

		getLocalList(e).add(e);
	}

	private void expand(SingleFileEventList q) {
		// TODO Auto-generated method stub

	}

	private SingleFileEventList getLocalList(Event e) {
		int[] pathElements = new int[] { e.date.getYear(), e.date.getMonthValue(), e.date.getDayOfMonth(),
				e.date.getHour(), e.date.getMinute() };

		for (int i = 0; i < pathElements.length; i++) {
			var f = new File(directory, concatPath(pathElements, i) + "/events.ser");

			if (f.exists()) {
				return new SingleFileEventList(parent, f);
			}
		}

		return new SingleFileEventList(parent, new File(directory, "events.ser"));
	}

	private String concatPath(int[] pathElements, int i) {
		String path = "";

		for (int j = 0; j <= i; j++) {
			path += pathElements[j] + "/";
		}

		return path;
	}

	@Override
	protected void forEachEvent(Consumer<Event> c) {
		// TODO Auto-generated method stub

	}

	@Override
	public Event forward() throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Event rewind() throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Event findEvent(long eventID) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Event remove(long index) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File getFileStoring(long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void forEachFile(Consumer<File> c) {
		// TODO Auto-generated method stub

	}

	@Override
	protected File nextSegment(File f) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected File previousSegment(File f) {
		// TODO Auto-generated method stub
		return null;
	}

}
