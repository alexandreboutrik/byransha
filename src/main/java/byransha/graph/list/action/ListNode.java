package byransha.graph.list.action;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

import byransha.graph.BNode;
import byransha.graph.ListItemPanel;
import byransha.graph.action.CreateNewListElement;
import byransha.graph.action.Export.CSVData;
import byransha.graph.list.action.export.ExportAsListOfIDs;
import byransha.graph.list.action.filter.RemoveSelected;
import byransha.graph.list.action.filter.RetainSelected;
import byransha.graph.list.action.map.MapToClassNode;
import byransha.graph.relection.ClassNode;
import byransha.ui.swing.ChatSheet;
import byransha.ui.swing.TextDisplayComponent;
import byransha.util.IntObjectBiConsumer;
import byransha.util.ListenableList;

public class ListNode<T extends BNode> extends BNode {
	String label;
	final public ListenableList<T> elements = new ListenableList<>();
	final public ListenableList<T> selection = new ListenableList<>();
	public Class<T> contentClass;

	public ListNode(BNode parent, String label, Class<T> contentClass) {
		super(parent);
		this.label = label;
		this.contentClass = contentClass;
	}

	@Override
	protected boolean acceptDrop(BNode droppedNode) {
		if (!contentClass.getClass().isAssignableFrom(droppedNode.getClass()))
			return false;

		return elements.add((T) droppedNode);
	}

	public ClassNode<T> contentClass() {
		return g().indexes.byClass.getClassNodeFor(contentClass);
	}

	public void selectAll() {
		selection.clear();
		selection.addAll(elements);
	}

	public void selectNone() {
		selection.clear();
	}

	public void invertSelection() {
		for (T e : elements) {
			if (selection.contains(e)) {
				selection.remove(e);
			} else {
				selection.add(e);
			}
		}
	}

	public void shuffle() {
		Collections.shuffle(elements);
	}

	@Override
	public void forEachOut(BiConsumer<BNode, String> consumer) {
		super.forEachOut(consumer);
		forEachOutInContent((i, o) -> consumer.accept(o, "" + i));
	}

	public void forEachOutInContent(IntObjectBiConsumer<BNode> consumer) {
		var l = elements;

		for (int i = 0; i < l.size(); ++i) {
			consumer.accept(i, l.get(i));
		}
	}

	@Override
	public void createActions() {
		cachedActions.elements.add(new Clear(this));
		cachedActions.elements.add(new CreateNewListElement(this, this));

		cachedActions.elements.add(new SortByString(this));
		cachedActions.elements.add(new SortByValue(this));
		cachedActions.elements.add(new SortByClass(this));

		cachedActions.elements.add(new Uniq(this));
		cachedActions.elements.add(new MapToClassNode(this));
		cachedActions.elements.add(new EDistribution(this));

		cachedActions.elements.add(new RetainSelected<>(this));
		cachedActions.elements.add(new RemoveSelected<>(this));

		cachedActions.elements.add(new DotAction(this));
		cachedActions.elements.add(new GeneratePlantUML(this));
		cachedActions.elements.add(new ExportAsListOfIDs(this));

		cachedActions.elements.add(new selectAll(this));
		cachedActions.elements.add(new selectNone(this));
		cachedActions.elements.add(new invertSelection(this));

		cachedActions.elements.add(new Shuffle(this));

		super.createActions();
	}

	@Override
	public void removeOut(BNode out) {
		elements.remove(out);
	}

	@Override
	public void toCSVStreams(List<CSVData> l, boolean printHeaders)
			throws IllegalArgumentException, IllegalAccessException {

		for (int i = 0; i < elements.size(); ++i) {
			var line = elements.get(i);
			line.fieldsToCSV(i == 0 ? printHeaders : false);
		}
	}

	public boolean isHeterogeneous() {
		return classes().size() <= 1;
	}

	public MultiValuedMap<Class<? extends BNode>, BNode> classes() {
		var r = new HashSetValuedHashMap<Class<? extends BNode>, BNode>();
		elements.forEach(e -> r.put(e.getClass(), e));
		return r;
	}

	@Override
	public String whatIsThis() {
		return "a list of nodes";
	}

	@Override
	public String toString() {
		return label == null ? "a list" : label;
	}

	public void select(int i) {
		selection.add(elements.get(i));
	}

	public void unselect(int i) {
		selection.remove(elements.get(i));
	}

	public List<T> getSelected() {
		return selection;
	}

	public void reset() {
		elements.clear();
		selection.clear();
	}

	public String toDot() {
		var s = new StringWriter();
		var pw = new PrintWriter(s);
		pw.println("digraph {");
		elements.forEach(e -> pw.println(e.id() + ";"));
		elements.forEach(
				e -> e.forEachOut((o, role) -> pw.println(e.id() + " -> " + o.id() + "[label=\"" + role + "\"]")));
		pw.println("}");
		return s.toString();
	}

	public List<T> get() {
		return elements;
	}

	public void set(List<T> l) {
		elements.clear();
		selection.clear();
		elements.addAll(l);
	}

	public boolean isSelected(T n) {
		return selection.contains(n);
	}

	private String label() {
		if (elements.size() == 0) {
			return "0 element";
		} else {
			return getSelected().size() + " selected element(s), among " + elements.size();
		}
	}

	@Override
	public void writeKishanView(ChatSheet sheet) {
		super.writeKishanView(sheet);
		writeToKishanView(sheet);
	}

	@Override
	protected void writeToKishanView(ChatSheet sheet) {
		var label = new TextDisplayComponent(g().translator, label());
		sheet.currentLine.add(label);
		sheet.newLine();
		final var line = sheet.currentLine;

		for (int i = 0; i < elements.size(); ++i) {
			line.add(new ListItemPanel(elements.get(i), this, i + 1, sheet.chat));
		}

		// sheet.newLine();
		elements.addListener(new ListenableList.Listener<T>() {

			@Override
			public void onAdded(int index, T element) {
				label.setText(label());
				line.add(new ListItemPanel(element, ListNode.this, index, sheet.chat), index);
			}

			@Override
			public void onRemoved(int index, T oldElement) {
				label.setText(label());
				line.remove(index);
			}

			@Override
			public void onSet(int index, T oldElement, T newElement) {
			}
		});

		selection.addListener(new ListenableList.Listener<T>() {

			@Override
			public void onAdded(int index, T element) {
				label.setText(label());
				((ListItemPanel) line.getComponent(elements.indexOf(element))).showSelectionStatus(true);
			}

			@Override
			public void onRemoved(int index, T element) {
				label.setText(label());
				((ListItemPanel) line.getComponent(elements.indexOf(element))).showSelectionStatus(false);
			}

			@Override
			public void onSet(int index, T oldElement, T newElement) {
				throw new IllegalStateException();
			}

		});
	}

}
