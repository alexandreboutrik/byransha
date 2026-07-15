package byransha.graph;

import java.lang.reflect.Method;

public class MethodAction extends Action {
	final Method m;

	public MethodAction(BNode parent, Method m) {
		super(parent, m.getDeclaringClass());
		this.m = m;
		this.hasButtonOnKishanView = m.isAnnotationPresent(ActionMethod.class);
	}

	@Override
	public String whatItDoes() {
		return m.getName();
	}

	@Override
	protected void impl() throws Throwable {
		m.invoke(parent);
	}

	@Override
	public boolean applies() {
		return true;
	}
}
