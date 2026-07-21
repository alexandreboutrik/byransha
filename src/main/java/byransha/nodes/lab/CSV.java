package byransha.nodes.lab;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class CSV extends ArrayList<List<String>> {
	public final List<String> headers;

	public CSV(String s, String sep) {
		var a = s.split("\"");

		for (int i = 1; i < a.length; i += 2) {
			a[i] = a[i].replace('\n', ' ');
			a[i] = a[i].replace('\r', ' ');
		}

		s = String.join("", a);
		var lines = new ArrayList<>(List.of(s.split("\n")));
		headers = List.of(lines.removeFirst().split(sep));
		lines.forEach(line -> add(new ArrayList<>(List.of(line.split(sep, -1)))));

		check();
	}

	public void check() {
		for (var lineIndex = 0; lineIndex < size(); ++lineIndex) {
			var e = get(lineIndex);

			if (e.size() != headers.size())
				throw new IllegalStateException(
						"line " + lineIndex + " has length " + e.size() + " instead of " + headers.size());
		}
	}

	public CSV(File f, String sep) throws IOException {
		this(Files.readString(f.toPath(), StandardCharsets.ISO_8859_1), ";");
	}

	public String get(int line, String col) {
		return get(line, headers.indexOf(col));
	}

	public String get(int line, int col) {
		return get(line).get(col);
	}

	public List<String> getColumn(String col) {
		return getColumn(headers.indexOf(col));
	}

	public List<String> getColumn(int i) {
		return stream().map(l -> l.get(i)).toList();
	}

	public int nbEmpty() {
		int n = 0;
		for (var a : this) {
			for (var b : a) {
				if (b.isEmpty())
					++n;
			}
		}
		return n;
	}
}
