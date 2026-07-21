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

import java.util.function.Function;

public class Conversion {
	public static int long2int(long n) {
		int intValue = (int) n;

		if (n != intValue)
			throw new Error("too big to be converted to int: " + n + " => " + intValue);

		return intValue;
	}

	public static int[] toIntArray(long... a) {
		int[] r = new int[a.length];

		for (int i = 0; i < a.length; ++i) {
			r[i] = long2int(a[i]);
		}

		return r;
	}

	public static boolean text2boolean(String s) {
		if (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")) {
			return true;
		} else if (s.equalsIgnoreCase("no") || s.equalsIgnoreCase("false")) {
			return false;
		}

		throw new NumberFormatException("invalid boolean: " + s);
	}

	private static final char[] hexAlphabet = "0123456789ABCDEF".toCharArray();

	public static char[] bytesToHex(byte[] b) {
		final char[] r = new char[hexAlphabet.length * 3 - 1];
		int i = 0;

		for (byte c : b) {
			r[i++] = hexAlphabet[(c >> 4) & 0x0f];
			r[i++] = hexAlphabet[c & 0x0f];

			if (i < r.length - 1) {
				r[i++] = ' ';
			}
		}

		return r;
	}

	public static interface Converter<FROM, TO> extends Function<FROM, TO> {
		boolean support(FROM from, Class<?> to);
	}

	public static byte[] intToBytes(int i) {
		return new byte[] { (byte) (i >>> 24), (byte) (i >>> 16), (byte) (i >>> 8), (byte) i };
	}

	public static int bytesToInt(byte[] b) {
		// Cuz int encode by complement-on-two
		// For a negative, signed left shift operation will Fill the upper part of the
		// binary with 1.
		// That‘s a question for us to combine the meaningful part.

		// Here, we execute a AND 0xFF operation, to implicitly convert a byte to int,
		// and fill the upper part of the binary with 0
		// So ,we got a positive number now.
		// The next step just execute OR operation to combine the four part as an
		// integer.
		return (b[0]) << 24 | (b[1] & 0xFF) << 16 | (b[2] & 0xFF) << 8 | (b[3] & 0xFF);
	}

}
