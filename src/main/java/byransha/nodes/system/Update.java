package byransha.nodes.system;

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
		System.exit(46); // tells the launch script to update the binaries
	}

	@Override
	public boolean applies() {
		return true;
	}
}