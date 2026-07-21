/* (C) Copyright 2009-2013 CNRS (Centre National de la Recherche Scientifique).

Licensed to the CNRS under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The CNRS licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.

*/

/* Contributors:

Luc Hogie (CNRS, I3S laboratory, University of Nice-Sophia Antipolis)
Aurelien Lancin (Coati research team, Inria)
Christian Glacet (LaBRi, Bordeaux)
David Coudert (Coati research team, Inria)
Fabien Crequis (Coati research team, Inria)
Grégory Morel (Coati research team, Inria)
Issam Tahiri (Coati research team, Inria)
Julien Fighiera (Aoste research team, Inria)
Laurent Viennot (Gang research-team, Inria)
Michel Syska (I3S, Université Cote D'Azur)
Nathann Cohen (LRI, Saclay)
Julien Deantoin (I3S, Université Cote D'Azur, Saclay)

*/

package byransha.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUtilities {

	private static final String regex = "([a-z])([A-Z]+)";
	private static final String replacement = "$1_$2";

	public static String camelToSnake(String s) {
		return s.replaceAll(regex, replacement).toLowerCase();
	}

	public static String base64(byte[] bytes) {
		return new String(Base64.getMimeEncoder().encode(bytes)).replace("\n", "").replace("\r", "");
	}

	public static byte[] unbase64(byte[] bytes) {
		return Base64.getMimeDecoder().decode(bytes);
	}

	public static List<String> matchesGroups(String s, String re) {
		Pattern r = Pattern.compile(re);
		Matcher m = r.matcher(s);

		if (m.find()) {
			int n = m.groupCount();
			var l = new ArrayList<String>(n);

			for (int i = 0; i < n; ++i) {
				l.add(m.group(i));
			}

			return l;
		} else {
			return null;
		}
	}

	public static List<String> split(String s, char c) {
		int i = 0;
		var r = new ArrayList<String>();

		while (true) {
			int p = s.indexOf(c, i);

			if (p < 0) {
				r.add(s.substring(i));
				return r;
			}

			r.add(s.substring(i, p));
			i = p + 1;
		}
	}

	public static List<List<String>> matchesGroups(List<String> lines, String re) {
		return lines.stream().map(s -> matchesGroups(s, re)).toList();
	}

	public static List<String> grep(List<String> lines, String re) {
		return lines.stream().filter(s -> s.matches(re)).toList();
	}

	public static List<String> lines(byte[] bytes) {
		return lines(new String(bytes));
	}

	public static String itemize(List<? extends Object> lines) {
		return bullets(lines, false);
	}

	public static String enumerate(List<? extends Object> lines) {
		return bullets(lines, true);
	}

	public static String bullets(List<? extends Object> lines, boolean order) {
		var b = new StringBuilder();

		for (int i = 0; i < lines.size(); ++i) {
			b.append("\t" + (order ? i + ".\t" : "- ") + lines.get(i));

			if (i < lines.size()) {
				b.append("\n");
			}
		}

		return b.toString();
	}

	public enum HORIZONTAL_ALIGNMENT {
		RIGHT, CENTER, LEFT
	}

	public enum VERTICAL_ALIGNMENT {
		TOP, MIDDLE, BOTTOM
	}

	public static int toInt(String s) {
		return Integer.valueOf(s);
	}

	public static long toLong(String s) {
		return Long.valueOf(s);
	}

	public static double toDouble(String s) {
		return Double.valueOf(s);
	}

	public static String box(String s) {
		return box(s, '*', s.length() + 4);
	}

	public static String box(String s, char c, int len) {
		String r = repeat(c, len) + '\n';
		int slen = Math.min(s.length(), len - 4);
		r += c + " " + s.substring(0, slen) + repeat(' ', len - 3 - slen) + c + '\n';
		r += repeat(c, len);
		return r;
	}

	public static long[] parseNumbers(char[] s, char separator) {
		long[] a = new long[getNumberOf(separator, s) + 1];
		int in = 0;

		for (int i = 0; i < s.length; ++i) {
			if (Character.isDigit(s[i])) {
				int n = s[i] - '0';

				while (++i < s.length && Character.isDigit(s[i])) {
					n = n * 10 + s[i] - '0';
				}

				a[in++] = n;
			}
		}

		return a;
	}

	public static int parseFileSize(String s) {
		return parseFileSize(s, 1024);
	}

	public static int parseFileSize(String s, int multiplier) {
		char lastLetter = s.charAt(s.length() - 1);

		if (lastLetter == 'b') {
			s = s.substring(0, s.length() - 1);
			lastLetter = s.charAt(s.length() - 1);
		}

		if (Character.isLetter(lastLetter)) {
			char unit = Character.toLowerCase(lastLetter);
			int base = Integer.valueOf(s.substring(0, s.length() - 1));

			if (unit == 'k') {
				return base * multiplier;
			} else if (unit == 'm') {
				return base * multiplier * multiplier;
			} else if (unit == 'g') {
				return base * multiplier * multiplier * multiplier;
			} else {
				throw new IllegalArgumentException("unknown unit '" + lastLetter + "'");
			}
		} else {
			return Integer.valueOf(s);
		}
	}

	public static String removeUselessDecimals(double i) {
		if (i == (int) i) {
			return String.valueOf((int) i);
		} else {
			return String.valueOf(i);
		}
	}

	public static void main(String... args) {
		System.out.println(parseNumbers("4 32 2 34 3".toCharArray(), ' ')[1]);
	}

	/*
	 * public static String toHumanString(final long n) { if (n < 0) { return "-" +
	 * toHumanString( - n); } else if (n < 1000) { return String.valueOf(n); } else
	 * { String s = String.valueOf(n); String units = "uKMGPH";
	 *
	 * int unit = (s.length() - 1) / 3;
	 *
	 * String a = s.substring(0, s.length() - 3 * unit);
	 *
	 * if (a.isEmpty()) { a = "0"; }
	 *
	 * String b = s.substring(s.length() - 3 * unit, s.length() - 3 * unit + 3 -
	 * a.length());
	 *
	 * String u = unit > 0 ? String.valueOf(units.charAt(unit)) : "";
	 *
	 * return a + "." + b + u; } }
	 */
	public static class HumanNumber {
		public String value;
		public char multiplier;

		@Override
		public String toString() {
			return value + multiplier;
		}
	}

	public static <T extends Comparable<T>> T min(T a, T b) {
		return a.compareTo(b) > 0 ? b : a;
	}

	public static <T extends Comparable<T>> T max(T a, T b) {
		return a.compareTo(b) > 0 ? a : b;
	}

	public static List<String> lines(String text) {
		return Arrays.asList(text.split("\\n"));
	}

	public static List<String> splitInLines2(String text) {
		List<String> l = new ArrayList();
		int p = 0;

		while (true) {
			int q = text.indexOf('\n', p);

			if (q < 0) {
				l.add(text.substring(p));
				break;
			} else {
				l.add(text.substring(p, q));
				p = q + 1;
			}
		}

		return l;
	}

	public static List<Integer> grep(List<String> lines, String pattern, boolean caseSensitive, boolean v) {
		List<Integer> newLines = new ArrayList<Integer>();

		Pattern re = caseSensitive ? Pattern.compile(pattern) : Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);

		for (int i = 0; i < lines.size(); ++i) {
			String line = lines.get(i);

			if ((!v && re.matcher(line).matches()) || (v && !re.matcher(line).matches())) {
				newLines.add(i);
			}
		}

		return newLines;
	}

	public static int indexOfRegexp(String lookIn, Pattern lookFor) {
		Matcher m = lookFor.matcher(lookIn);

		if (m.find()) {
			return m.start();
		} else {
			return -1;
		}
	}

	public static List<String> getLinesAtIndexes(List<String> lines, List<Integer> lineNumbers, int indexShift) {
		List<String> selectedLines = new ArrayList<String>();

		for (int thisLineNumber : lineNumbers) {
			selectedLines.add(lines.get(thisLineNumber + indexShift));
		}

		return selectedLines;
	}

	public static String prefixEachLineBy(String text, String prefix) {
		if (!text.startsWith("\n")) {
			text = prefix + text;
		}

		return text.replaceAll("\n", "\n" + prefix);
	}

	public static void prefixEachLineByLineNumber(List<String> lines, String separator, int start) {
		int width = (int) Math.log10(lines.size()) + 1;

		for (int i = 0; i < lines.size(); ++i) {
			lines.set(i, flushRight(String.valueOf(i + start), width, '0') + separator + lines.get(i));
		}
	}

	public static String toBinary(int n, int numberOfDigits, boolean useSpacing) {
		StringBuffer buf = new StringBuffer();

		while (numberOfDigits-- > 0) {
			if (n % 2 == 0) {
				buf.append('0');
			} else {
				buf.append('1');
			}

			n /= 2;

			if (useSpacing) {
				if (numberOfDigits % 4 == 0) {
					buf.append(' ');
				}
			}
		}

		buf.reverse();
		return buf.toString();
	}

	public static String toHex(byte[] bytes) {
		return toHex(bytes, ' ');
	}

	public static String toHex(byte[] bytes, char sep) {
		StringBuilder buf = new StringBuilder(bytes.length * 3);

		for (int i = 0; i < bytes.length; ++i) {
			byte b = bytes[i];

			if (i > 0) {
				buf.append(sep);
			}

			String s = toHexString(b);

			if (s.length() == 1) {
				s = "0" + s;
			}

			buf.append(s);
		}

		return buf.toString();
	}

	public static byte[] fromHex(String s) {
		if (s.length() % 2 != 0)
			throw new IllegalArgumentException("input text doesn't have an even number of characters");

		byte[] r = new byte[s.length()];

		for (int i = 0; i < r.length; i += 2) {
			String a = s.substring(i, i + 2);
			r[i] = Byte.decode("#" + a);
		}

		return r;
	}

	public static String toHexString(byte b) {
		return Integer.toHexString(b & 0xFF);
	}

	public static boolean isDouble(String s) {
		try {
			Double.valueOf(s);
			return true;
		} catch (NumberFormatException ex) {
			return false;
		}
	}

	public static boolean isInt(String s) {
		try {
			Integer.valueOf(s);
			return true;
		} catch (NumberFormatException ex) {
			return false;
		}
	}

	public static boolean isLong(String s) {
		try {
			Long.valueOf(s);
			return true;
		} catch (NumberFormatException ex) {
			return false;
		}
	}

	/**
	 * @return a nice representation of the name of the given array class.
	 *         org.lucci.Boh -> org.lucci.Boh [String9 - String[]
	 */
	public static String getNiceClassName(Class clazz) {
		if (clazz.isArray()) {
			return getNiceClassName(clazz.getComponentType()) + "[]";
		} else {
			return clazz.getName();
		}
	}

	public static String getClassNameWithoutPackage(Class clazz) {
		if (clazz.isArray()) {
			return getNiceClassName(clazz.getComponentType()) + "[]";
		} else {
			String s = clazz.getName();
			return s.substring(s.lastIndexOf('.') + 1);
		}
	}

	public static String replaceVariableValues(String s, Map<String, String> variableValues) {
		int a = s.indexOf("${");

		if (a < 0) {
			return s;
		} else {
			int b = s.indexOf('}', a + 2);

			if (b < 0) {
				throw new IllegalArgumentException("unterminated variable reference: '}' expected");
			} else {
				String name = s.substring(a + 2, b);

				if (name.matches("[a-zA-Z_]+")) {
					String value = variableValues.get(name);

					if (value == null) {
						throw new IllegalArgumentException("variable undeclared: " + name);
					} else {
						return s.substring(0, a) + value + replaceVariableValues(s.substring(b + 1), variableValues);
					}
				} else {
					throw new IllegalArgumentException("invalid variable name: '" + name + "'");
				}
			}
		}
	}

	/**
	 * @return the full name of the method described by the given elements.
	 */
	public static String getNiceMethodName(Class target, String name, Class[] argTypes) {
		StringBuilder buf = new StringBuilder();
		buf.append(target.getName());
		buf.append('#');
		buf.append(name);
		buf.append('(');

		if (argTypes != null && argTypes.length > 0) {
			for (int i = 0; i < argTypes.length; ++i) {
				buf.append(' ');
				buf.append(getNiceClassName(argTypes[i]));

				if (i < argTypes.length - 1) {
					buf.append(',');
				}
			}

			buf.append(' ');
		}

		buf.append(')');
		return buf.toString();
	}

	public static String normalizePropertyName(String s) {
		StringBuffer buf = new StringBuffer();
		int len = s.length();

		for (int i = 0; i < len; ++i) {
			char c = s.charAt(i);

			if (isPropertyChar(c)) {
				if (i == 0) {
					buf.append(Character.toLowerCase(c));
				} else if (i > 0 && !isPropertyChar(s.charAt(i - 1))) {
					buf.append(Character.toUpperCase(c));
				} else {
					buf.append(c);
				}
			}
		}

		return buf.toString();
	}

	public static boolean isPropertyChar(char c) {
		return Character.isLetterOrDigit(c) || c == '_';
	}

	public static String capitalizeWord(String s) {
		if (s.isEmpty()) {
			return "";
		} else {
			return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
		}
	}

	public static final Collection<String> exceptions = Arrays
			.asList(new String[] { "a", "the", "an", "and", "to", "from", "on", "of", "by" });

	public static String capitalizeAllWords(String s) {
		s = s.toLowerCase();
		List<String> words = Arrays.asList(s.split(" "));

		if (!words.isEmpty()) {
			words.set(0, capitalizeWord(words.get(0)));

			for (int i = 1; i < words.size(); ++i) {
				String w = words.get(i).toLowerCase();
				words.set(i, exceptions.contains(w) ? w : capitalizeWord(w));
			}
		}

		return concatene(words, " ");
	}

	public static String invertCase(String s) {
		StringBuffer buf = new StringBuffer();
		int len = s.length();

		for (int i = 0; i < len; ++i) {
			char c = s.charAt(i);

			if (Character.isLetter(c)) {
				if (Character.isUpperCase(c)) {
					buf.append(Character.toLowerCase(c));
				} else {
					buf.append(Character.toUpperCase(c));
				}
			} else {
				buf.append(c);
			}
		}

		return buf.toString();
	}

	public static String repeat(String s, int count) {
		StringBuilder buf = new StringBuilder();

		for (int i = 0; i < count; ++i) {
			buf.append(s);
		}

		return buf.toString();
	}

	public static String repeat(char c, int count) {
		StringBuilder buf = new StringBuilder();

		for (int i = 0; i < count; ++i) {
			buf.append(c);
		}

		return buf.toString();
	}

	public static String flush(Object o, HORIZONTAL_ALIGNMENT alignment, int lineLenght, char fillChar) {
		String s = o.toString();

		if (s.length() > lineLenght)
			throw new IllegalArgumentException("'" + o + "'.length() >  " + lineLenght);

		if (alignment == HORIZONTAL_ALIGNMENT.LEFT) {
			return s + TextUtilities.repeat(String.valueOf(fillChar), lineLenght - s.length());
		} else if (alignment == HORIZONTAL_ALIGNMENT.RIGHT) {
			return TextUtilities.repeat(String.valueOf(fillChar), lineLenght - s.length()) + s;
		} else if (alignment == HORIZONTAL_ALIGNMENT.CENTER) {
			int leftMargin = (lineLenght - s.length()) / 2;
			int rightMargin = lineLenght - leftMargin;
			return TextUtilities.repeat(String.valueOf(fillChar), leftMargin) + s
					+ TextUtilities.repeat(String.valueOf(fillChar), rightMargin);
		} else {
			throw new IllegalArgumentException("alignment == null");
		}

	}

	public static String flushRight(Object o, int lineLenght, char fillChar) {
		String s = o.toString();

		if (s.length() > lineLenght)
			s = s.substring(0, lineLenght);

		return TextUtilities.repeat(String.valueOf(fillChar), lineLenght - s.length()) + s;
	}

	public static String flushLeft(Object o, int lineLenght, char fillChar) {
		String s = o.toString();

		if (s.length() > lineLenght)
			s = s.substring(0, lineLenght);

		return s + TextUtilities.repeat(String.valueOf(fillChar), lineLenght - s.length());
	}

	public static String concatene(Collection strings, String separator) {
		StringBuilder b = new StringBuilder();

		Iterator<String> i = strings.iterator();

		while (i.hasNext()) {
			Object s = i.next();
			b.append(s.toString());

			if (i.hasNext()) {
				b.append(separator);
			}
		}

		return b.toString();
	}

	public static <E> String concat(String separator, Iterable<E> elements, Function<E, String> toString) {
		StringBuilder b = new StringBuilder();
		var i = elements.iterator();

		while (i.hasNext()) {
			var e = i.next();
			// System.out.println(e);

			b.append(toString.apply(e));

			if (i.hasNext()) {
				b.append(separator);
			}
		}

		return b.toString();
	}

	public static <E> String concat(String separator, Iterable<E> elements) {
		return concat(separator, elements, e -> e.toString());
	}

	public static List<String> wrap(String text, int size) {
		List<String> unwrappables = Arrays.asList(text.split("\\s+"));

		for (int i = 0; i < unwrappables.size(); ++i) {
		}

		List<String> lines = new Vector<String>();
		lines.add("");

		for (String unwrappable : unwrappables) {
			if (!lines.get(lines.size() - 1).isEmpty()) {
				unwrappable = ' ' + unwrappable;
			}

			if (lines.get(lines.size() - 1).length() + unwrappable.length() <= size) {
				lines.set(lines.size() - 1, lines.get(lines.size() - 1) + unwrappable);
			} else {
				lines.add(unwrappable.trim());
			}
		}

		return lines;
	}

	public static String defaultAlphabet = "azertyuiopqsdfghjklmwxcvbnazertyuiopAZERTYUIOPQSDFGHJKLMWXCVBN123456789?0@&\"'(!)-$€%£?,;.:/=+* ";

	public static char pickUpOneRandomChar(Random random) {
		return pickUpOneRandomChar(defaultAlphabet, random);
	}

	public static char pickUpOneRandomChar(String alphabet, Random random) {
		return alphabet.charAt((int) (random.nextDouble() * alphabet.length()));
	}

	public static String generateRandomString(String alphabet, int length, Random random) {
		StringBuilder b = new StringBuilder();

		while (length-- >= 0) {
			b.append(pickUpOneRandomChar(alphabet, random));
		}

		return b.toString();
	}

	public static int computeLevenshteinDistance(String s, String t) {
		if (s == null || t == null) {
			throw new NullPointerException();
		}

		/*
		 * The difference between this impl. and the previous is that, rather than
		 * creating and retaining a matrix of size s.length()+1 by t.length()+1, we
		 * maintain two single-dimensional arrays of length s.length()+1. The first, d,
		 * is the 'current working' distance array that maintains the newest distance
		 * cost counts as we iterate through the characters of String s. Each time we
		 * increment the index of String t we are comparing, d is copied to p, the
		 * second int[]. Doing so allows us to retain the previous cost counts as
		 * required by the algorithm (taking the minimum of the cost count to the left,
		 * up one, and diagonally up and to the left of the current cost count being
		 * calculated). (Note that the arrays aren't really copied anymore, just
		 * switched...this is clearly much better than cloning an array or doing a
		 * System.arraycopy() each time through the outer loop.)
		 *
		 * Effectively, the difference between the two implementations is this one does
		 * not cause an out of memory condition when calculating the LD over two very
		 * large strings.
		 */

		int n = s.length(); // length of s
		int m = t.length(); // length of t

		if (n == 0) {
			return m;
		} else if (m == 0) {
			return n;
		}

		int p[] = new int[n + 1]; // 'previous' cost array, horizontally
		int d[] = new int[n + 1]; // cost array, horizontally
		int _d[]; // placeholder to assist in swapping p and d

		// indexes into strings s and t
		int i; // iterates through s
		int j; // iterates through t

		char t_j; // jth character of t

		int cost; // cost

		for (i = 0; i <= n; i++) {
			p[i] = i;
		}

		for (j = 1; j <= m; j++) {
			t_j = t.charAt(j - 1);
			d[0] = j;

			for (i = 1; i <= n; i++) {
				cost = s.charAt(i - 1) == t_j ? 0 : 1;
				// minimum of cell to the left+1, to the top+1, diagonally left
				// and up +cost
				d[i] = Math.min(Math.min(d[i - 1] + 1, p[i] + 1), p[i - 1] + cost);
			}

			// copy current distance counts to 'previous row' distance counts
			_d = p;
			p = d;
			d = _d;
		}

		// our last action in the above loop was to switch d and p, so p now
		// actually has the most recent cost counts
		return p[n];
	}

	public static String removeComments(String s) {
		return s.replaceAll("#.*(\\n|$)", "");
	}

	public static int getNumberOf(char c, char[] s) {
		int n = 0;

		for (int i = 0; i < s.length; ++i) {
			if (s[i] == c) {
				++n;
			}
		}

		return n;
	}

	public static int getNumberOf(char c, String s) {
		int n = 0;
		int p = -1;

		while ((p = s.indexOf(c, p + 1)) != -1) {
			++n;
		}

		return n;
	}

	public static String seconds2date(long s, boolean discardZeros) {
		long h = s / 3600;
		s %= 3600;
		long m = s / 60;
		s %= 60;
		return (h == 0 && discardZeros ? "" : h + "h ") + (m == 0 && discardZeros ? "" : m + "min ") + s + "s";
	}

	public static String milliseconds2date(long l, boolean discardZeros) {
		long h = l / 3600000;
		l %= 3600000;

		long m = l / 60000;
		l %= 60000;

		long s = l / 1000;
		s %= 1000;

		long ms = s;

		return (h == 0 && discardZeros ? "" : h + "h") + (m == 0 && discardZeros ? "" : m + "m")
				+ ((s == 0 && discardZeros ? "" : s + "s")) + ms + "ms";
	}

	public static boolean toBoolean(String s) {
		if (s.equals("true") || s.equals("yes")) {
			return true;
		} else if (s.equals("false") || s.equals("no")) {
			return false;
		} else {
			throw new IllegalStateException("don't know how interpret this as a boolean: " + s);
		}
	}

	public static String toString(Object o) {
		if (o == null) {
			return "null";
		} else if (o instanceof String) {
			return (String) o;
		} else if (o instanceof byte[]) {
			return Arrays.toString((byte[]) o);
		} else if (o instanceof boolean[]) {
			return Arrays.toString((boolean[]) o);
		} else if (o instanceof char[]) {
			return Arrays.toString((char[]) o);
		} else if (o instanceof short[]) {
			return Arrays.toString((short[]) o);
		} else if (o instanceof int[]) {
			return Arrays.toString((int[]) o);
		} else if (o instanceof float[]) {
			return Arrays.toString((float[]) o);
		} else if (o instanceof double[]) {
			return Arrays.toString((double[]) o);
		} else if (o instanceof long[]) {
			return Arrays.toString((long[]) o);
		} else if (o instanceof Throwable) {
			Throwable ex = (Throwable) o;
			StringWriter w = new StringWriter();
			PrintWriter pw = new PrintWriter(w);
			ex.printStackTrace(pw);
			pw.flush();
			return w.getBuffer().toString();
		} else if (o instanceof Object[]) {
			StringBuilder b = new StringBuilder();
			b.append("[");
			Object[] a = (Object[]) o;

			for (int i = 0; i < a.length; ++i) {
				b.append(toString(a[i]));

				if (i < a.length - 1)
					b.append(", ");
			}

			b.append("]");
			return b.toString();
		} else {
			return o.toString();
		}
	}

	public static boolean parseBoolean(String s) {
		if (s.equals("yes") || s.equals("true")) {
			return true;
		} else if (s.equals("no") || s.equals("false")) {
			return false;
		} else {
			throw new IllegalArgumentException("cannot be interpreted as a boolean: " + s);
		}
	}

	public static String[] getParagraphs(String text) {
		return text.split("\n\n");
	}

	public static String unquote(String s, char quoteChar) {
		if (s.charAt(0) == quoteChar && s.charAt(s.length() - 1) == quoteChar) {
			return s.substring(1, s.length() - 1);
		} else {
			return s;
		}
	}

	public static String repeat(String line, int n, String sep) {
		StringBuilder b = new StringBuilder(line.length() * n + sep.length() * (n - 1));

		for (int i = 0; i < n; ++i) {
			if (i > 0)
				b.append(sep);

			b.append(line);
		}

		return b.toString();
	}

	public static int[] toInts(String[] a) {
		int[] r = new int[a.length];

		for (int i = 0; i < r.length; ++i) {
			r[i] = Integer.valueOf(a[i]);
		}

		return r;
	}

	public static boolean isASCIIPrintable(String line) {
		int len = line.length();

		for (int i = 0; i < len; ++i) {
			if (!isAsciiPrintable(line.charAt(i))) {
				return false;
			}
		}

		return true;
	}

	public static boolean isAsciiPrintable(char ch) {
		return ch >= 32 && ch < 127 || Character.isWhitespace(ch);
	}

	public static String exception2string(Throwable t) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(bos);
		t.printStackTrace(ps);
		return new String(bos.toByteArray());
	}

	// ThisIsAnExample -> this is an example
	public static String f(String s) {
		var r = new StringBuilder();
		int len = s.length();

		for (int i = 0; i < len; ++i) {
			var c = s.charAt(i);

			if (i == 0 && Character.isUpperCase(c)) {
				r.append(Character.toLowerCase(c));
			} else if (i > 0 && Character.isUpperCase(c) && Character.isLowerCase(s.charAt(i - 1))) {
				r.append('_');
				r.append(Character.toLowerCase(c));
			} else {
				r.append(c);
			}
		}

		return r.toString();
	}

}