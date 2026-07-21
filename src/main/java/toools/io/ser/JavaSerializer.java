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
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public class JavaSerializer<E> extends Serializer<E> {
	public static final JavaSerializer<?> instance = new JavaSerializer<>();

	@Override
	public E read(InputStream is) throws IOException {
		try {
			DataInputStream d = new DataInputStream(is);
			int size = d.readInt();
			byte[] b = new byte[size];
			d.readFully(b);

			ObjectInputStream oos = new ObjectInputStream(new ByteArrayInputStream(b)) {
				{
					enableResolveObject(true);
				}

				@Override
				protected Object resolveObject(Object obj) throws IOException {
					return replaceAtDeserialization(obj);
				}
			};

			Object o = oos.readObject();
			oos.close();
			return (E) o;
		} catch (ClassNotFoundException e) {
			throw new IOException(e);
		}
	}

	protected Object replaceAtDeserialization(Object obj) {
		return obj;
	}

	protected Object replaceAtSerialization(Object obj) {
		return obj;
	}

	@Override
	public void write(E o, OutputStream os) throws IOException {
		ByteArrayOutputStream bo = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bo) {
			{
				enableReplaceObject(true);
			}

			@Override
			protected Object replaceObject(Object obj) throws IOException {
				return replaceAtSerialization(obj);
			}
		};
		oos.writeObject(o);
		oos.close();
		byte[] buf = bo.toByteArray();
		DataOutputStream dos = new DataOutputStream(os);
		dos.writeInt(buf.length);
		dos.write(buf);
	}

	@Override
	public String getMIMEType() {
		return "ser";
	}

	@Override
	public boolean isBinary() {
		return true;
	}
}
