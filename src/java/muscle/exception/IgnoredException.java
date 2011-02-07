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

package muscle.exception;


/**
indicates an erroneous command (i.e. method call) which has been skipped
@author Jan Hegewald
*/
public class IgnoredException extends RuntimeException {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	public IgnoredException() {

	}
	public IgnoredException(String message) {
		super(message);
	}
	public IgnoredException(Throwable cause) {
		super(cause);
	}
	public IgnoredException(String message, Throwable cause) {
		super(message, cause);
	}
}