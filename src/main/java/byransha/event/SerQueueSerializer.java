package byransha.event;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import byransha.graph.BGraph;

public class SerQueueSerializer implements EventQueueSerializer {

	@Override
	public String ext() {
		return "ser";
	}

	@Override
	public void write(InMemoryEventList q, File out) throws IOException {
		var os = new ObjectOutputStream(new FileOutputStream(out));
		os.writeObject(q);
		os.close();
	}

	@Override
	public InMemoryEventList read(File in, BGraph g) throws Exception {
		var is = new ObjectInputStream(new FileInputStream(in));
		var q = (InMemoryEventList) is.readObject();
		is.close();
		return q;
	}
}
