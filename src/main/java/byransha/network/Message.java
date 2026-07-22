package byransha.network;

import java.util.ArrayList;
import java.util.List;

public class Message {
	public List<String> route = new ArrayList<>();
	public long targetNodeId;
	public Object content;
}