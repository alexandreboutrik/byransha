package byransha.nodes.primitive.file;

import java.io.File;

import byransha.graph.ProcedureAction;
import byransha.nodes.primitive.StringNode;
import byransha.nodes.primitive.file.openFile.file;

public class renameFile extends ProcedureAction<FileNode> {
	StringNode newName;

	public renameFile(FileNode inputNode) {
		super(inputNode, file.class);
		this.newName = new StringNode(this, inputNode.file.getName(), ".+");
		hasButtonOnKishanView=true;
	}

	@Override
	public String whatItDoes() {
		return "ranames the file";
	}

	@Override
	public void impl() throws Throwable {
		inputNode.file.renameTo(new File(inputNode.file.getParent(), newName.get()));
	}

	@Override
	public boolean applies() {
		return inputNode.file.exists();
	}

}
