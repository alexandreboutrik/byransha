package byransha.graph.index;

import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

import byransha.graph.BGraph;
import byransha.graph.BNode;
import byransha.graph.Index;
import byransha.graph.relection.ClassNode;
import byransha.util.Stop;

public class ByClass extends Index {

	public MultiValuedMap<Class, BNode> m = new HashSetValuedHashMap<>();
	public final BGraph g;

	public ByClass(BGraph g) {
		super(g);
		this.g = g;
	}

	public <C extends BNode> C forEachNodeAssignableTo(Class<C> nodeClass, Function<C, Stop> f) {
		for (var c : m.keySet()) {
			if (nodeClass.isAssignableFrom(c)) {
				for (var n : m.get(c)) {
					if (f.apply((C) n) == Stop.yes) {
						return (C) n;
					}
				}
			}
		}

		return null;
	}

	public <C extends BNode> C findFirst(Class<C> c, Predicate<C> p) {
		return forEachNodeAssignableTo(c, n -> Stop.stopIf(p.test(n)));
	}
	/*
	 * public <C extends BNode> C findFirstOr(Class<C> c, Predicate<C> p,
	 * Supplier<C> defaultValue) { var r = findFirst(c, p); return r == null &&
	 * defaultValue != null ? defaultValue.get() : r; }
	 */

	@Override
	public void add(final BNode n) {
		ensureThereAreClassesForTheHierarchyOf(n.getClass());
		n.ascendSuperClassesUntil(n.getClass(), BNode.class, clazz -> m.put(clazz, n));
	}

	private void ensureThereAreClassesForTheHierarchyOf(Class c) {
		if (c == null || c == ClassNode.class) {
			return;
		}

		var cn = getClassNodeFor(c);

		if (cn != null) {
			return;
		}

		cn = new ClassNode(g, c);
		m.put(ClassNode.class, cn);
		ensureThereAreClassesForTheHierarchyOf(c.getSuperclass());

		for (var i : c.getInterfaces()) {
			ensureThereAreClassesForTheHierarchyOf(i);
		}
	}

	public ClassNode getClassNodeFor(Class clazz) {
		for (var cn : m.get(ClassNode.class)) {
			if (((ClassNode) cn).representedClass == clazz) {
				return (ClassNode) cn;
			}
		}

		return new ClassNode(g, clazz);
		// throw new IllegalStateException("class node should be registered: " +
		// getClass());
	}

	@Override
	public void delete(BNode n) {
		n.ascendSuperClassesUntil(n.getClass(), BNode.class, clazz -> m.removeMapping(clazz, n));
	}

	@Override
	public String strategy() {
		return "class";
	}

}