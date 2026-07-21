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

package toools.io.ser;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class TextSerializer<E> extends Serializer<E> {
	@Override
	public E read(InputStream is) throws IOException {
		String s = new String(is.readAllBytes());
		return toObject(s);
	}

	protected abstract E toObject(String s);

	@Override
	public void write(E o, OutputStream os) throws IOException {
		os.write(toString((E) o).getBytes());
	}

	protected String toString(E o) {
		return o.toString();
	}

	@Override
	public String getMIMEType() {
		return "txt";
	}

	@Override
	public boolean isBinary() {
		return false;
	}

	public static final TextSerializer<Integer> Int32 = new TextSerializer<Integer>() {
		@Override
		protected Integer toObject(String s) {
			return Integer.parseInt(s.trim());
		}
	};

	public static final TextSerializer<Double> Float64 = new TextSerializer<Double>() {
		@Override
		protected Double toObject(String s) {
			return Double.parseDouble(s.trim());
		}
	};

	public static final TextSerializer<Long> Int64 = new TextSerializer<Long>() {
		@Override
		protected Long toObject(String s) {
			return Long.parseLong(s.trim());
		}
	};

	public static final TextSerializer<Boolean> Bool = new TextSerializer<Boolean>() {
		@Override
		protected Boolean toObject(String s) {
			return Boolean.parseBoolean(s);
		}
	};

	public static final TextSerializer<String> String = new TextSerializer<String>() {
		@Override
		protected String toObject(String s) {
			return s;
		}
	};

	public static final TextSerializer<int[]> IntArray = new TextSerializer<int[]>() {
		@Override
		protected int[] toObject(String s) {
			String[] lines = s.split("\n");
			int[] r = new int[lines.length];

			for (int i = 0; i < r.length; ++i) {
				r[i] = Integer.parseInt(lines[i]);
			}

			return r;
		}

		@Override
		protected String toString(int[] o) {
			StringBuilder r = new StringBuilder();

			for (int i = 0; i < o.length; ++i) {
				r.append(o[i]);
			}

			return r.toString();
		}
	};

	public static final TextSerializer<Color> Color = new TextSerializer<Color>() {
		@Override
		protected Color toObject(String s) {
			return java.awt.Color.decode(s);
		}

		@Override
		protected String toString(Color c) {
			return c.toString();
		}
	};
}
