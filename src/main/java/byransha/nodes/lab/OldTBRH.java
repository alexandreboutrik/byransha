package byransha.nodes.lab;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import byransha.nodes.primitive.DateNode;
import byransha.nodes.primitive.EmailNode;
import byransha.nodes.primitive.LongNode;
import byransha.nodes.primitive.PhoneNumberNode;
import byransha.nodes.primitive.StringNode;
import byransha.util.Stop;

class OldTBRH {
	public void loadOLDTBRH(Lab i3s, File inputDir) throws IOException {
		loadPersonnelIT(i3s, inputDir);
	}

	private static void loadPersonnelIT(Lab i3s, File extractionDir) throws IOException {

		var csv = new CSV(new File(extractionDir, "personneI3S_IT.csv"), ";");

		for (var l : csv) {
			System.out.println(l);

			var person = new Person(i3s); // new Person(graph);

			if (l.set(0, "").equals("member")) {
				var position = new Position(person);
				position.employer = i3s;
				person.positions.elements.add(position);
			}

			person.name.set(l.set(1, ""));
			person.familyNameBeforeMariage.set(l.set(2, ""));
			person.firstName.set(l.set(3, ""));
			person.birthDate.set(DataLake.parseDate(l.set(4, "")));
			person.cityOfBirth.set(l.set(5, ""));
			// person.etatCivil.countryOfBirth.set(l.set(6, null));
			// person.etatCivil.nationality.set(l.set(7, null));
			person.address.text.set(l.set(8, ""));
			var inter = new PhoneNumberNode(person);
			inter.set(l.set(9, ""));
			person.phoneNumbers.elements.add(inter);

			var officeName = l.set(15, "");

			for (var campusName : List.of(l.set(10, ""), l.set(11, ""))) {
				if (!campusName.isBlank()) {
					var campus = i3s.g().indexes.byClass.forEachNodeAssignableTo(Campus.class,
							n -> Stop.stopIf(n.name.get() != null && n.name.get().equalsIgnoreCase(campusName)));

					if (campus != null && !officeName.isBlank()) {
						for (var b : campus.buildings.elements) {
							var office = b.findOffice(officeName);

							if (office != null) {
								person.offices.elements.add((Room) office);
							}
						}
					}
				}
			}

			for (var phoneNumber : List.of(l.set(12, ""), l.set(13, ""), l.set(14, ""))) {
				var n = new PhoneNumberNode(person);
				n.set(phoneNumber);
				person.phoneNumbers.elements.add(n);
			}

			person.badgeNumber.set(l.set(16, ""));
			person.website.set(l.set(17, ""));
			l.set(18, ""); // remove Fax number person.faxNumber.set();
			var email = new EmailNode(person, "");
			email.set(l.set(19, ""));
			person.emailAddresses.elements.add(email);
			String researchGroupName = l.set(20, "").trim();

			if (!researchGroupName.isEmpty()) {
				person.structures.elements.add(findFirst(i3s, researchGroupName));
			}

			boolean phd = l.set(21, "").equalsIgnoreCase("oui");
			String phdDate = l.set(22, "");

			if (phdDate != null && phdDate != "") {
				person.phdDate.set(DataLake.parseDate(phdDate));
			} else if (phd) {
				person.phdDate.set(null);
			}
			// System.err.println(l.stream().map(e-> e == null ? "" : "-").toList());
			var startDate = l.set(30, "");
			var endDate = l.set(31, "");

			for (var i : List.of(25, 26)) {
				var employer = l.set(i, "").trim();
				var position = new Position(person);

				if (!employer.isEmpty() && !employer.equals("autre")) {
					position.employer = findFirst(i3s, employer);
				}

				person.positions.elements.add(position); // new Position(graph);

				var corps = l.set(i - 2, "");
				// person.position.status = graph.find(Status(g), s ->
				// s.name.get().equals(corps)
				// );

				if (!startDate.isBlank()) {
					var startDateNode = new DateNode(position);
					startDateNode.set(DataLake.parseDate(startDate));
					person.positions.elements.getLast().from = startDateNode;
				}

				if (!endDate.isBlank()) {
					var endDateNode = new DateNode(position);
					endDateNode.set(DataLake.parseDate(endDate));
					person.positions.elements.getLast().to = endDateNode;
				}
			}

			person.enposte = l.set(27, null).equals("en poste");

			try {
				var quotite = new LongNode(person);
				quotite.set(Long.valueOf(l.set(29, "")));
				person.quotite = quotite;
			} catch (NumberFormatException err) {

			}

			var researchActivity = new StringNode(person);
			researchActivity.set(l.set(33, null));
			person.researchActivity = researchActivity;
			System.out.println(l);

		}
	}

	private static Structure findFirst(Structure i3s, String name) {
		if (name.equals("laboratoire"))
			name = "I3S";

		if (name.equals("UNS") || name.equals("UCA"))
			name = "UniCA";

		String ss = name;
		var r = i3s.g().indexes.byClass.forEachNodeAssignableTo(Structure.class,
				n -> Stop.stopIf(n.name.get() != null && n.name.get().equalsIgnoreCase(ss)));
		Objects.requireNonNull(r, name);
		return r;
	}
}
