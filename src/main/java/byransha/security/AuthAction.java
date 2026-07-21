package byransha.security;

import byransha.graph.Action;
import byransha.graph.BGraph;
import byransha.nodes.primitive.StringNode;
import byransha.nodes.system.User;
import byransha.security.Authenticator.security;
import byransha.util.Stop;

public class AuthAction extends Action {
	public final StringNode username = new StringNode(this, "", ".+");
	public final StringNode password = new StringNode(this, "", ".+");

	public AuthAction(BGraph g) {
		super(g, security.class);
	}

	@Override
	public void impl() {
		var u = username.get();
		var p = password.get();
		var g = g();

		if (!(u == null || u.isBlank() || p == null || p.isBlank() || !g.authenticatorMethod.test(u, p))) {
			g.setCurrentUser(g.indexes.byClass.forEachNodeAssignableTo(User.class,
					uu -> Stop.stopIf(uu.name.get().equals(u))));
		}
	}

	@Override
	public String toString() {
		return g().authenticatorMethod.authenticationMethod();
	}

	@Override
	public boolean applies() {
		return true;
	}

	@Override
	public String whatItDoes() {
		return g().authenticatorMethod.authenticationMethod() + " authentication";
	}

}