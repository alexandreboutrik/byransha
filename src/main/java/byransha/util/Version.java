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

import java.util.Arrays;

public class Version implements Comparable<Version> {
	private int[] numbers = new int[] { 0, 0, 1 };

	public void set(String s) {
		String[] a = s.split("\\.");

		if (a.length == numbers.length) {
			for (int i = 0; i < 3; ++i) {
				numbers[i] = Integer.parseInt(a[i]);
			}
		} else {
			throw new IllegalArgumentException("invalid version: " + s);
		}
	}

	public void set(int major, int minor, int revision) {
		numbers[0] = major;
		numbers[1] = minor;
		numbers[2] = revision;
	}

	public static enum Level {
		major, minor, revision, same
	};

	public int get(int i) {
		return numbers[i];
	}

	public int get(Level l) {
		return get(l.ordinal());
	}

	public void set(int i, int n) {
		if (n < 0)
			throw new IllegalArgumentException("should be positive");

		numbers[i] = n;
	}

	public void set(Level l, int n) {
		set(l.ordinal(), n);
	}

	public void upgrade(Level l) {
		if (l == Level.same)
			return;

		int n = l.ordinal();
		++numbers[n];

		while (++n < numbers.length) {
			numbers[n] = 0;
		}
	}

	@Override
	public String toString() {
		return numbers[0] + "." + numbers[1] + "." + numbers[2];
	}

	@Override
	public boolean equals(Object obj) {
		return Arrays.equals(numbers, ((Version) obj).numbers);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(numbers);
	}

	public boolean isNewerThan(Version v) {
		return compareTo(v) > 0;
	}

	public int compareTo(Version v) {
		if (!compliesWith(v))
			throw new IllegalArgumentException(
					"version do not comply: " + this + " and " + v);

		for (int i = 0; i < numbers.length; ++i) {
			int c = Integer.compare(numbers[i], v.numbers[i]);

			if (c != 0) {
				return c;
			}
		}

		return 0;
	}

	public boolean compliesWith(Version v) {
		return numbers.length == v.numbers.length;
	}

}
