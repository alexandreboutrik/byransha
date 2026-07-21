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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.io.Serializable;

public abstract class Serializer<E> {
	public abstract E read(InputStream is) throws IOException;

	public abstract void write(E o, OutputStream os) throws IOException;

	public abstract String getMIMEType();

	public Object fromBytes(byte[] bytes) {
		try {
			return read(new ByteArrayInputStream(bytes));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public byte[] toBytes(E o) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			write(o, bos);
			return bos.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Serializer[] serializers = new Serializer[] { new JavaSerializer() };

	public static class UnserializationResult {
		public Serializer protocol;
		public Object object;
	}

	public static Serializer getDefaultSerializer() {
		return serializers[0];
	}

	public static UnserializationResult unserialize(InputStream is) throws IOException {
		int i = is.read();
		UnserializationResult r = new UnserializationResult();
		r.protocol = serializers[i];
		r.object = r.protocol.read(is);
		return r;
	}

	private static int indexOf(Serializer p) {
		for (int i = 0; i < serializers.length; ++i) {
			if (serializers[i] == p) {
				return i;
			}
		}

		throw new IllegalArgumentException("unknown serialization protocol");
	}

	public static class CustomObjectInputStream extends ObjectInputStream {
		private ClassLoader classLoader;

		public CustomObjectInputStream(InputStream in, ClassLoader classLoader) throws IOException {
			super(in);
			this.classLoader = classLoader;
		}

		protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
			// System.out.println("using classloader " +
			// classLoader.getClass().getName() + " load class " +
			// desc.getName());
			return Class.forName(desc.getName(), false, classLoader);
		}
	}

	public E clone(E a) {
		return (E) fromBytes(toBytes(a));
	}

	public static final Object EOS = new Serializable() {
		@Override
		public boolean equals(Object o) {
			return o.getClass() == getClass();
		}
	};

	public abstract boolean isBinary();

}
