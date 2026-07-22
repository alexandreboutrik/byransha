package byransha.nodes.system;

import java.io.File;

import byransha.graph.Category;
import byransha.graph.ProcedureAction;

final class Update extends ProcedureAction<Byransha> {
	public static class byransha extends Category {
	}

	public Update(Byransha inputNode) {
		super(inputNode, byransha.class);
	}

	@Override
	public String whatItDoes() {
		return "restart";
	}

	@Override
	public void impl() throws Throwable {
		var elements = System.getProperty("java.classpath").split(System.getProperty("path.separator"));

		if (elements.length != 1) {
			throw new IllegalStateException("Update can only be called from a jar file");
		}

		var jarFile = new File(elements[0]);

		System.exit(46); // tells the launch script to restart
	}

	@Override
	public boolean applies() {
		// only applies if the program is running from a jar file
		return System.getProperty("java.class.path").split(System.getProperty("path.separator")).length == 1;
	}
}