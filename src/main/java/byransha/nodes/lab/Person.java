package byransha.nodes.lab;

import javax.swing.JComponent;
import javax.swing.JLabel;

import byransha.graph.BNode;
import byransha.graph.BusinessNode;
import byransha.graph.ShowInKishanView;
import byransha.graph.list.action.ListNode;
import byransha.nodes.Factory;
import byransha.nodes.primitive.BooleanNode;
import byransha.nodes.primitive.DateNode;
import byransha.nodes.primitive.EmailNode;
import byransha.nodes.primitive.LongNode;
import byransha.nodes.primitive.LongNode.Bounds;
import byransha.nodes.primitive.PhoneNumberNode;
import byransha.nodes.primitive.StringNode;
import byransha.nodes.primitive.URLNode;
import byransha.nodes.system.ChatNode;

public class Person extends BusinessNode {
	@ShowInKishanView
	public Genre genre;
	@ShowInKishanView
	public ListNode<Position> positions = new ListNode<Position>(this, "positions", Position.class);
	@ShowInKishanView
	public URLNode pics = new URLNode(this, "");
	@ShowInKishanView
	public final BooleanNode hdr = new BooleanNode(this, null);
	@ShowInKishanView
	public final BadgeNode badgeNumber = new BadgeNode(this);
	@ShowInKishanView
	public URLNode website = new URLNode(this, null);
	@ShowInKishanView
	public final ListNode<Structure> structures = new ListNode<>(this, "structures", Structure.class);
	@ShowInKishanView
	public DateNode phdDate = new DateNode(this);
	@ShowInKishanView
	public ListNode<PhoneNumberNode> phoneNumbers = new ListNode<PhoneNumberNode>(this, "phone number(s)",
			PhoneNumberNode.class);
	@ShowInKishanView
	public ListNode<EmailNode> emailAddresses = new ListNode<EmailNode>(this, "email adresses", EmailNode.class);
	@ShowInKishanView
	public ListNode<Room> offices = new ListNode<Room>(this, "offices", Room.class);
	@ShowInKishanView
	public LongNode quotite = new LongNode(this);
	public boolean enposte;
	@ShowInKishanView
	public ListNode<Publication> publications = new ListNode<>(this, "publications", Publication.class);
	@ShowInKishanView
	public final StringNode orcid = new StringNode(this, null, "^(\\d{4}-){3}\\d{3}(\\d|X)$");
	@ShowInKishanView
	public final StringNode authID = new StringNode(this, null, "^A\\d{7}$");
	@ShowInKishanView
	public StringNode researchActivity;
	@ShowInKishanView
	public StringNode name = new StringNode(this, null, ".+");
	@ShowInKishanView
	public StringNode firstName = new StringNode(this, null, ".+");
	@ShowInKishanView
	public StringNode familyNameBeforeMariage = new StringNode(this);
	@ShowInKishanView
	public StringNode cityOfBirth = new StringNode(this, null, ".+");
	@ShowInKishanView
	public AddressNode address = new AddressNode(this);
	public Country countryOfBirth;

	public ListNode<Nationality> nationality = new ListNode<Nationality>(this, "nationalities", Nationality.class);
	public DateNode birthDate = new DateNode(this);
	public PhoneNumberNode telephone = new PhoneNumberNode(this);

	@Factory
	public Person(BNode parent) {
		super(parent);
		quotite.setBounds(new Bounds(0, 100));
	}

	@Override
	public String toString() {
		return firstName.toString() + " " + name.toString();
	}

	@Override
	public String whatIsThis() {
		return "a physical person working in the lab";
	}

	@Override
	public JComponent getListItemComponent(ChatNode chat) {
		return new JLabel(firstName.get() + " " + name.get());
	}

}
