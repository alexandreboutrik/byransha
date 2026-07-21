package byransha.nodes.primitive.file;

import byransha.graph.ProcedureAction;
import byransha.nodes.primitive.file.openFile.file;

public class delete extends ProcedureAction<FileNode> {

	public delete(FileNode inputNode) {
		super(inputNode, file.class);
		hasButtonOnKishanView = true;
	}

	@Override
	public String whatItDoes() {
		return "deletes the file";
	}

	@Override
	public void impl() {
		inputNode.file.delete();
	}

	@Override
	public boolean applies() {
		return inputNode.file.exists();
	}

}
