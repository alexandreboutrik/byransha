package byransha.graph;

import java.awt.Color;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import byransha.ai.QueryIA;
import byransha.graph.action.Delete;
import byransha.graph.action.Export;
import byransha.graph.action.Export.CSVData;
import byransha.graph.action.FreezingAction;
import byransha.graph.action.JumpToAnotherNode;
import byransha.graph.action.search.Search;
import byransha.graph.action.search.SearchRegexp;
import byransha.graph.action.search.SearchText;
import byransha.graph.list.action.ListNode;
import byransha.graph.relection.ClassNode;
import byransha.network.Message;
import byransha.nodes.primitive.LongNode;
import byransha.nodes.primitive.StringNode;
import byransha.nodes.primitive.ValuedNode;
import byransha.nodes.primitive.file.FileNode;
import byransha.nodes.system.ChatNode;
import byransha.nodes.system.User;
import byransha.ui.swing.ChatSheet;
import byransha.ui.swing.CircleComponent;
import byransha.ui.swing.ColorPalette;
import byransha.ui.swing.ErrorIndicator;
import byransha.ui.swing.MenuBuilder;
import byransha.ui.swing.TextDisplayComponent;
import byransha.ui.swing.TranslatableTextArea;
import byransha.ui.swing.Utils;
import byransha.ui.swing.WrapPanel;
import byransha.util.Base62;
import byransha.util.ByUtils;
import byransha.util.ListenableList;
import byransha.util.Stop;
import byransha.util.TriConsumer;

public abstract class BNode {
	final public static JsonNodeFactory factory = new JsonNodeFactory(true);
	final public static ObjectMapper objectMapper = new ObjectMapper();

	public final BNode parent;
	public boolean readOnly;
	protected boolean resilient = false;
	public long id = -1;
	public BGraph graph;
	protected ListNode<Action> cachedActions;

	protected BNode(BNode parent) {
		if (!(this instanceof BGraph) && parent == null)
			throw new NullPointerException();
		this.parent = parent;

		var g = g();

		if (g != null && g.indexes != null) {
			g.indexes.add(this);
		}

		if (enclosingBusinessNode() != null) {
			// g().eventList.add(new NewNodeEvent<>(this));
		}
	}

	protected void handle(Message msg) {
	};

	public String findRoleOf(BNode n) {
		var foundRole = new String[1];
		forEachOut((out, role) -> {
			if (foundRole[0] != null && out == n) {
				foundRole[0] = role;
			}
		});
		return foundRole[0];
	}

	public BGraph g() {
		return parent != null ? parent.g() : null;
	}

	public BusinessNode enclosingBusinessNode() {
		if (this instanceof BusinessNode bn) {
			return bn;
		} else if (parent != null) {
			return parent.enclosingBusinessNode();
		} else {
			return null;
		}
	}

	public boolean isResilient() {
		if (resilient) {
			return true;
		} else if (parent != null) {
			return parent.isResilient();
		} else {
			return false;
		}
	}

	public Action enclosingActionNode() {
		if (this instanceof Action bn) {
			return bn;
		} else if (parent != null) {
			return parent.enclosingActionNode();
		} else {
			return null;
		}
	}

	public int depth() {
		return parent == null ? 0 : parent.depth() + 1;
	}

	public List<String> rolePathElements() {
		var r = new ArrayList<String>();

		for (BNode a = this; a.parent != null; a = a.parent) {
			r.add(a.parent.findRoleOf(a));
		}

		Collections.reverse(r);
		return r;
	}

	public String rolePath() {
		return rolePathElements().stream().collect(Collectors.joining("/"));
	}

	public ListNode<BNode> path() {
		var r = new ListNode<BNode>(this, "path", BNode.class);

		for (BNode a = this; a != null; a = a.parent) {
			r.elements.add(a);
		}

		Collections.reverse(r.elements);
		return r;
	}

	protected <N extends BNode> ListNode<N> inverseRelation(String label, Class<N> c, Function<N, ListNode> f) {
		var r = new ListNode<N>(this, label, c);

		for (var n : g().indexes.byClass.m.get(c)) {
			var nn = (N) n;
			var remoteList = f.apply(nn);

			if (remoteList.elements.contains(BNode.this)) {
				r.elements.add(nn);
			}
		}

		r.elements.addListener(new ListenableList.Listener<N>() {

			@Override
			public void onSet(int index, N oldElement, N p) {
				throw new UnsupportedOperationException("not supported");
			}

			@Override
			public void onRemoved(int index, N n) {
				f.apply(n).elements.remove(BNode.this);
			}

			@Override
			public void onAdded(int index, N n) {
				f.apply(n).elements.add(BNode.this);
			}
		});

		return r;
	}

	protected <N extends BNode> ListNode<N> inverseRelation2(String label, Class<N> c, Function<N, BNode> f) {
		var r = new ListNode<N>(this, label, c);

		for (var n : g().indexes.byClass.m.get(c)) {
			var nn = (N) n;

			if (f.apply(nn) == BNode.this) {
				r.elements.add(nn);
			}
		}

		r.elements.addListener(new ListenableList.Listener<N>() {

			@Override
			public void onSet(int index, N oldElement, N p) {
				throw new UnsupportedOperationException("not supported");
			}

			@Override
			public void onRemoved(int index, N n) {
				throw new UnsupportedOperationException("not supported");
			}

			@Override
			public void onAdded(int index, N n) {
				throw new UnsupportedOperationException("not supported");
			}
		});

		return r;
	}

	public int computeLongestPathLength() {
		var r = bfs(Long.MAX_VALUE, n -> true, (n, d) -> {
		});
		return r.longestDistance();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " #" + idAsText();
	}

	public List<Action<?>> actions() {
		if (cachedActions == null) {
			cachedActions = new ListNode<>(this, "actions for node " + this, Action.class);
			createActions();

			for (var m : getClass().getMethods()) {
				if (m.isAnnotationPresent(ActionMethod.class)) {
					cachedActions.elements.add(new MethodAction(this, m));
				}
			}
		}

		return (List<Action<?>>) (List) cachedActions.get();
	}

	public void invalidateActionCache() {
		cachedActions = null;
	}

	public void delete() {
		g().indexes.delete(this);
	}

	public int sizeOf() {
		return ByUtils.sizeOfFields(this);
	}

	public void toCSVStreams(List<CSVData> l, boolean printHeaders)
			throws IllegalArgumentException, IllegalAccessException {
		var c = new CSVData();
		c.name = "fields";
		c.data = fieldsToCSV(printHeaders);
		l.add(c);
	}

	public final String fieldsToCSV(boolean printHeaders) throws IllegalArgumentException, IllegalAccessException {
		var sw = new StringWriter();
		var pw = new PrintWriter(sw);
		fieldsToCSV(pw, printHeaders);
		var s = sw.toString();
		pw.close();
		return s;
	}

	private void fieldsToCSV(PrintWriter ps, boolean printHeaders)
			throws IllegalArgumentException, IllegalAccessException {
		var fields = new ArrayList<Field>();

		if (printHeaders) {
			forEachOutInFields(getClass(), BNode.class, (f, o, ro) -> fields.add(f));
			ps.println('#' + fields.stream().map(f -> f.getName()).collect(Collectors.joining(", ")));
		}

		for (int i = 0; i < fields.size(); ++i) {
			var f = fields.get(i);
			BNode out = (BNode) f.get(this);
			ps.print(out.toString());

			if (i < fields.size() - 1) {
				ps.print(';');
			} else {
				ps.println();
			}
		}
	}

	public void removeOut(BNode out) {
		forEachOutInFields(getClass(), BNode.class, (f, o, ro) -> {
			try {
				if (o == out) {
					f.set(this, null);
				}
			} catch (IllegalAccessException err) {
				BGraph graph = g();
				if (graph != null && graph.errorLog != null) {
					graph.errorLog.add(err);
				} else {
					err.printStackTrace();
				}
			}
		});
	}

	public void forEachOutInFields(Class<? extends BNode> from, Class<? extends BNode> until,
			TriConsumer<Field, BNode, Boolean> consumer) {
		ascendSuperClassesUntil(from, until, c -> {
			for (var f : c.getDeclaredFields()) {
				if (f.isAnnotationPresent(ShowInKishanView.class)) {
					try {
						f.setAccessible(true);
						var out = f.get(this);
						var isFinal = (f.getModifiers() & Modifier.FINAL) != 0;

						if (out instanceof BNode outNode) {
							consumer.accept(f, outNode, isFinal);
						} else if (out != null) {
							var outNode = instantiateRenderingNodeFor(out);
							outNode.readOnly = true;
							consumer.accept(f, outNode, isFinal);
						}

					} catch (IllegalArgumentException | IllegalAccessException e) {
						BGraph graph = g();
						if (graph != null && graph.errorLog != null) {
							graph.errorLog.add(e);
						} else {
							e.printStackTrace();
						}
					}
				}
			}
		});
	}

	public void forEachOutInMethods(Class<? extends BNode> from, Class<? extends BNode> until,
			BiConsumer<Method, BNode> consumer) {
		for (var m : getClass().getMethods()) {
			if (m.isAnnotationPresent(ShowInKishanView.class)) {
				try {
					var out = m.invoke(this);

					if (out instanceof BNode outNode) {
						consumer.accept(m, outNode);
					} else if (out != null) {
						var outNode = instantiateRenderingNodeFor(out);
						outNode.readOnly = true;
						consumer.accept(m, outNode);
					}
				} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
					BGraph graph = g();
					if (graph != null && graph.errorLog != null) {
						graph.errorLog.add(e);
					} else {
						System.err
								.println("Erreur pour la méthode " + m.getName() + " sur " + this.getClass().getName());
						e.printStackTrace();
					}
				}
			}
		}
	}

	private BNode instantiateRenderingNodeFor(Object o) {
		if (o instanceof File f) {
			return new FileNode(this, f);
		} else if (o instanceof List l) {
			var ln = new ListNode(this, "list", Object.class);
			ln.elements.addAll(l);
			return ln;
		} else if (o instanceof Long l) {
			return new LongNode(this, l);
		} else {
			return new StringNode(this, o.toString(), null);
		}
	}

	public void forEachOut(BiConsumer<BNode, String> consumer) {
		forEachOutInFields(getClass(), BNode.class, (f, o, ro) -> consumer.accept(o, f.getName()));
		forEachOutInMethods(getClass(), BNode.class, (m, o) -> consumer.accept(o, m.getName()));
	}

	public void createActions() {
		// cachedActions.add(new Back(g, this));
		cachedActions.elements.add(new QueryIA(this));
		cachedActions.elements.add(new SeeClassNode(this));
		cachedActions.elements.add(new CopyIDToClipboard(this));
		cachedActions.elements.add(new FreezingAction(this));
		cachedActions.elements.add(new JumpToAnotherNode(this));
		cachedActions.elements.add(new Export(this));
		cachedActions.elements.add(new Delete(this));
		cachedActions.elements.add(new Search(this));
		cachedActions.elements.add(new SearchText(this));
		cachedActions.elements.add(new SearchRegexp(this));
		cachedActions.elements.add(new OpenInNewChat(this));
	}

	public void ascendSuperClassesUntil(Class<? extends BNode> from, Class<? extends BNode> until,
			Consumer<Class<? extends BNode>> consumer) {

		if (!until.isAssignableFrom(from))
			throw new IllegalArgumentException("from " + from + " to " + until);

		for (Class c = from; c != until; c = c.getSuperclass()) {
			consumer.accept(c);
		}

		consumer.accept(until);
	}

	public String whatIsThis() {
		return type().whatItRepresents();
	}

	public BFSResult bfs(long maxDistance, Predicate<BNode> nodeFilter, ObjIntConsumer<BNode> consumer) {
		List<BNode> q = new ArrayList<>();
		var r = new BFSResult();

		BNode c = this;
		q.add(c);
		r.distances.put(c, 0);

		while (!q.isEmpty()) {
			c = q.removeFirst();
			int d = r.distances.getInt(c);

			if (d > maxDistance) { // went too far
				break;
			}

			if (d > 0 && consumer != null) { // don't expose source node
				consumer.accept(c, d);
			}

			final var c_tmp = c;

			c.forEachOut((o, role) -> {
				if (o != null && !r.visited.contains(o)) {
					r.visited.add(o);

					if (nodeFilter.test(c_tmp)) {
						q.add(o);
						r.distances.put(o, d + 1);
					}
				}
			});
		}

		return r;
	}

	public final boolean canSee(User user) {
		if (user == null)
			return true;

		for (var r : user.roles.elements) {
			if (r.isAllowedToSee(this)) {
				return true;
			}
		}

		return false;
	}

	public final boolean canEdit(User user) {
		if (isReadOnly())
			return false;

		if (user == null)
			return true;

		for (var r : user.roles.elements) {
			if (r.isAllowedToEdit(this)) {
				return true;
			}
		}

		return false;
	}

	public boolean canCreate(User user) {
		return true;
	}

	public final long id() {
		return id;
	}

	@Override
	public final int hashCode() {
		return Long.hashCode(id());
	}

	@Override
	public final boolean equals(Object obj) {
		return this == obj;
	}

	public final Color getColor() {
		return ColorPalette.forClass(getClass(), g().swing.colorStyle.style);
	}

	public final Color getBackgroundColor() {
		var c = getColor();
		int alpha = (int) g().swing.transparencyForNodeBackground.get().longValue();
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
	}

	public Icon getIcon() {
		var bytes = getIconBytes();
		return bytes == null ? null : new ImageIcon();
	}

	public byte[] getIconBytes() {
		return null;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public final List<NodeError> errors() {
		var errs = new ArrayList<NodeError>();
		fillErrors(errs);
		return errs;
	}

	protected void fillErrors(List<NodeError> errs) {
		forEachOutInFields(getClass(), BNode.class, (f, v, ro) -> {
			if (v instanceof ValuedNode vn) {
				errs.addAll(vn.errors());
			}
		});
	}

	public ObjectNode describeAsJSON() {
		return toJSONNode(1);
	}

	public ObjectNode toJSONNode(int depth) {
		if (depth < 0)
			return null;

		ObjectNode r = new ObjectNode(factory);
		r.put("id", idAsText());
		r.put("class", getClass().getName());
		r.put("color", ByUtils.toHex(getColor()));
		r.put("toString", toString());

		var iconBytes = getIconBytes();

		if (iconBytes != null) {
			r.put("icon", Base64.getEncoder().encode(getIconBytes()));
		}

		r.put("whatIsThis", whatIsThis());
		r.put("canSee", canSee(g().currentUser()));
		r.put("canEdit", canEdit(g().currentUser()));
		r.set("actions",
				new ArrayNode(null, actions().stream().map(e -> (JsonNode) new TextNode(e.idAsText())).toList()));
		r.set("errors", new ArrayNode(null, errors().stream().map(err -> (JsonNode) new TextNode(err.msg)).toList()));

		var outsNode = new ObjectNode(factory);
		forEachOutInFields(getClass(), BNode.class,
				(f, out, ro) -> outsNode.put(f.getName(), out != null ? out.idAsText() : ""));
		r.set("outs", outsNode);

		return r;
	}

	public Action findAction(String actionName) {
		for (var a : actions()) {
			if (a.technicalName().equals(actionName)) {
				return a;
			}
		}

		return null;
	}

	public String idAsText() {
		return Base62.encode(id());
	}

	public ClassNode<?> type() {
		return g().indexes.byClass.getClassNodeFor(getClass());
	}

	public void set(Field f, BNode newValue) throws IllegalArgumentException, IllegalAccessException {
		f.set(this, newValue);
	}

	public String t(String s) {
		var translation = g().translator.translate(s);
		return translation == null ? s : translation;
	}

	public void writeKishanView(ChatSheet sheet) {
		int fieldNameSize = 10;// fieldMaxLength();

		forEachOutInFields(getClass(), BNode.class, (f, out, readOnly) -> {
			fillLine(sheet.currentLine, f, sheet, out, fieldNameSize);
			sheet.newLine();
		});

		forEachOutInMethods(getClass(), BNode.class, (method, out) -> {
			fillLine(sheet.currentLine, method, sheet, out, fieldNameSize);
			sheet.newLine();
		});

		var actionsWithButtons = actions().stream().filter(Action::hasButtonOnKishanView).toList();

		if (!actionsWithButtons.isEmpty()) {
			sheet.newLine();

			for (var a : actionsWithButtons) {
				if (a.hasButtonOnKishanView()) {
					var b = new JButton(a.whatItDoes());
					b.setEnabled(a.applies());
					b.addActionListener(e -> sheet.chat.append(a));
					sheet.appendToCurrentLine(b);
				}
			}
		}
	}

	private void fillLine(WrapPanel currentLine, Member m, ChatSheet sheet, BNode out, int left) {
		var roleComponent = new TextDisplayComponent(g().translator, m.getName() + ":");
		roleComponent.setColumns(left);
		roleComponent.setToolTipText(m.getName());

		currentLine.add(roleComponent);

		if (out != null) {
			currentLine.add(out.createBall(18, 2, ((ChatSheet) sheet).chat));
			currentLine.add(new ErrorIndicator(out));
			out.writeToKishanView(sheet);
		} else {
			sheet.appendToCurrentLine("field has no value");
		}

		if (m instanceof Field field) {
			Utils.idDropTarget(g(), roleComponent, droppedNode -> set(field, droppedNode));

			var popup = new JPopupMenu();
			var setToNull = new JMenuItem("unset");
			setToNull.addActionListener(e -> {
				try {
					set(field, null);
					sheet.currentLine.removeAll();
					fillLine(currentLine, m, sheet, out, left);
					sheet.doLayout();
					sheet.revalidate();
				} catch (Throwable e1) {
					BGraph graph = g();
					if (graph != null && graph.errorLog != null) {
						graph.errorLog.add(e1);
					} else {
						e1.printStackTrace();
					}
				}
			});
			var replace = new JMenuItem("see candidates");
			replace.addActionListener(e -> {
				var list = new ListNode(parent, "all nodes of class " + field.getType().getName(),
						(Class) field.getType());
				g().indexes.byClass.forEachNodeAssignableTo((Class) field.getType(), a -> {
					list.elements.add(a);
					return Stop.no;
				});
				new ChatNode(g().currentUser()).append(list);
			});

			if (!this.readOnly) {
				popup.add(out == null ? replace : setToNull);
			}

			roleComponent.setComponentPopupMenu(popup);
		}
	}

	protected void writeToKishanView(ChatSheet sheet) {
		sheet.appendToCurrentLine(getSmallComponent(sheet.chat));
	}

	public JComponent createBall(int diameter, int border, ChatNode chat) {
		var c = new CircleComponent(diameter, getColor());
		c.setBorderWidth(border);
		c.setOpaque(false);
		c.setFocusable(false);
		var tooltip = "<html>" + whatIsThis() + "<br><ul><li>" + idAsText() + "<li>" + getClass().getName()
				+ "</ul></html>";
		c.setToolTipText(tooltip);
		// SelectableTooltip.addSelectableTooltip(c,tooltip);
		Utils.idDropTarget(g(), c, droppedNode -> acceptDrop(droppedNode));

		DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(c, DnDConstants.ACTION_COPY,
				e -> e.startDrag(DragSource.DefaultCopyDrop, new StringSelection(idAsText())));

		c.setComponentPopupMenu(MenuBuilder.buildPopupMenu(actions(), chat));

		c.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					chat.append(BNode.this);
				}
			}
		});

		return c;
	}

	protected boolean acceptDrop(BNode droppedNode) {
		return false;
	}

	public JComponent getListItemComponent(ChatNode chat) {
		return getSmallComponent(chat);
	}

	protected JComponent getSmallComponent(ChatNode chat) {
		var ta = new TranslatableTextArea(g().translator);
		ta.setToolTipText(whatIsThis());
		ta.setText(toString());
		ta.setEditable(false);
		return ta;
	}
}
