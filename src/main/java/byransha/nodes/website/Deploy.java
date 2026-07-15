package byransha.nodes.website;

import java.io.IOException;
import java.nio.file.Files;

import byransha.graph.Category;
import byransha.graph.list.action.FunctionAction;
import byransha.nodes.primitive.file.FileNode;

public class Deploy extends FunctionAction<Website, FileNode> {
	FileNode directory;

	public static class website extends Category {
	}

	public Deploy(Website website) {
		super(website, website.class);
	}

	@Override
	public String whatItDoes() {
		return "deploy the website";
	}

	@Override
	public void impl() throws IOException {
		Files.writeString(directory.file.toPath(), inputNode.toHTMLPage().toHTML());
		result = directory;
	}

	@Override
	public boolean applies() {
		return true;
	}

}
