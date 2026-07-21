package byransha.nodes.primitive;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import byransha.graph.list.action.FunctionAction;
import byransha.nodes.primitive.file.FileNode;
import byransha.nodes.primitive.file.openFile.file;

public final class saveNodeAction extends FunctionAction<TextNode, FileNode> {
	StringNode fileNameNode = new StringNode(this, "example.txt", ".+\\..+");

	protected saveNodeAction(TextNode textNode) {
		super(textNode, file.class);
		hasButtonOnKishanView = true;
	}

	@Override
	public void impl() throws IllegalArgumentException, IllegalAccessException, IOException {
		var path = Path.of(fileNameNode.getOrDefault(inputNode + "-" + inputNode.id() + ".txt"));
		Files.write(path, inputNode.get().getBytes());
		var fileNode = new FileNode(this);
		fileNode.file = path.toFile();
		result = fileNode;
	}

	@Override
	public String whatItDoes() {
		return "save this text to a file";
	}

	@Override
	public String toString() {
		return "Save";
	}

	@Override
	public boolean applies() {
		return true;
	}
}