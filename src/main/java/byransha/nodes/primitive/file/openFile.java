package byransha.nodes.primitive.file;

import java.awt.Desktop;

import byransha.graph.Category;
import byransha.graph.ProcedureAction;

public class openFile extends ProcedureAction<FileNode> {

	public static class file extends Category {
	}

	public openFile(FileNode f) {
		super(f, file.class);
		hasButtonOnKishanView = true;
	}

	@Override
	public String whatItDoes() {
		return "open the file";
	}

	@Override
	public void impl() throws Throwable {
		Desktop.getDesktop().open(inputNode.file);
	}

	@Override
	public String toString() {
		return "open";
	}

	@Override
	public boolean applies() {
		return inputNode.file.exists();
	}

}