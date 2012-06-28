/*
Copyright 2008,2009 Complex Automata Simulation Technique (COAST) consortium

GNU Lesser General Public License

This file is part of MUSCLE (Multiscale Coupling Library and Environment).

    MUSCLE is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    MUSCLE is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with MUSCLE.  If not, see <http://www.gnu.org/licenses/>.
*/

package muscle.core.conduit.filter;

import java.io.Serializable;
import muscle.util.serialization.ByteJavaObjectConverter;

/**
serialize to a byte array
@author Jan Hegewald
*/
public class SerializeFilter<E extends Serializable> extends AbstractFilter<E,byte[]> {
	private final ByteJavaObjectConverter<E> converter = new ByteJavaObjectConverter<E>();
	protected void apply(E subject) {
		put(converter.serialize(subject));
	}
}
