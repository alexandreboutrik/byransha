package byransha.network;

import java.util.ArrayList;
import java.util.List;

public class Message {
	public List<String> route = new ArrayList<>();
	byte[] data; // encrypted and zipped
	public long targetNodeId;
}