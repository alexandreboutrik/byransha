package byransha.graph;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import byransha.event.EventList;
import byransha.event.SingleFileEventList;
import byransha.graph.index.AllIndexes;
import byransha.graph.relection.ClassNode;
import byransha.network.NetworkAgent;
import byransha.nodes.lab.Genre.Female;
import byransha.nodes.lab.Genre.Male;
import byransha.nodes.lab.Genre.NotGenred;
import byransha.nodes.system.Byransha;
import byransha.nodes.system.JVMNode;
import byransha.nodes.system.OSNode;
import byransha.nodes.system.User;
import byransha.security.AuthAction;
import byransha.security.Authenticator;
import byransha.security.LdapAuthenticator;
import byransha.translate.GoogleTranslator;
import byransha.translate.Translator;
import byransha.ui.swing.SwingFrontend;
import io.github.classgraph.ClassGraph;

public class BGraph extends BNode {
	@ShowInKishanView
	private User currentUser = new User(this, "guest");

	public AllIndexes indexes = new AllIndexes(this);
	@ShowInKishanView
	public final AllIndexesNode indexesNode = new AllIndexesNode(this);

	public Authenticator authenticatorMethod = new LdapAuthenticator(null, null, null, readOnly);
	@ShowInKishanView
	public AuthAction authenticator = new AuthAction(this);

	@ShowInKishanView
	public BNode application;
	@ShowInKishanView
	public final JVMNode jvm = new JVMNode(this);
	@ShowInKishanView
	public final Byransha byransha = new Byransha(this);
	@ShowInKishanView
	public final OSNode os = new OSNode(this);
	@ShowInKishanView
	public final ErrorLog errorLog = new ErrorLog(this);
	@ShowInKishanView
	public final EventList eventList = new SingleFileEventList(this,
			new File(System.getProperty("user.home"), "byransha-events.bin"));
	// public WebServer webServer;
	// public ByranshaWebSocketServer webSocketServer;

	@ShowInKishanView
	public SwingFrontend swing;
	@ShowInKishanView
	public final NetworkAgent networkAgent;
	@ShowInKishanView
	public final Translator translator = new GoogleTranslator(this);
	// public final Authenticate auth = new LdapAuthenticator(this);

	public final List<CurrentUserListener> userSwitchingListeners = new ArrayList<>();

	class graph extends Category {
	}

	public BGraph(File directory, int port) throws Exception {
		super(null);
		// indexes.add(this);
		this.networkAgent = new NetworkAgent(this, port);
		new Male(this);
		new Female(this);
		new NotGenred(this);

		var visitor = new VisitorRole(this);
		var admin = new AdminRole(this);

		currentUser.roles.elements.add(admin);
	}

	public void setCurrentUser(User newUser) {
		if (newUser != currentUser) {
			this.currentUser = newUser;
			userSwitchingListeners.forEach(l -> l.userSwitchedTo(currentUser, newUser));
		}
	}

	@Override
	public BGraph g() {
		return this;
	}

	public User getCurrentUser() {
		return currentUser;
	}

	@Override
	public void createActions() {
		super.createActions();
	}

	@Override
	public String whatIsThis() {
		return "a graph";
	}

	@Override
	public String toString() {
		return "super node";
	}

	public User currentUser() {
		return currentUser;
	}

	@ShowInKishanView
	public List<ClassNode> businessClasses() {
		return classesIn(application.getClass().getPackage(), BusinessNode.class);
	}

	public List<ClassNode> classesIn(Package p, Class superclass) {
		var r = new ArrayList<ClassNode>();

		for (var c : new ClassGraph().enableAllInfo().acceptPackages(p.getName()).scan().getAllClasses()
				.loadClasses()) {
			if (superclass.isAssignableFrom(c) && (c.getModifiers() & java.lang.reflect.Modifier.ABSTRACT) == 0
					&& c.getDeclaringClass() == null) {
				try {
					var constr = c.getConstructor(BNode.class);

					if (constr != null) {
						r.add(g().indexes.byClass.getClassNodeFor(c));
					}
				} catch (NoSuchMethodException err) {
					// Ignore classes without the required constructor
				}
			}
		}

		return r;
	}

}
