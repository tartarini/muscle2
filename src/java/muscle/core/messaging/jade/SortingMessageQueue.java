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

package muscle.core.messaging.jade;

import jade.lang.acl.ACLMessage;

import java.util.AbstractCollection;



/**
custom message queue for a jade.core.Agent which sorts arriving messages to pure jade acl messages and other messages
@author Jan Hegewald
*/
public class SortingMessageQueue extends jade.core.PublicMessageQueue {

	private AbstractCollection<DataMessage> nonACLQueue;


	//
	public SortingMessageQueue(AbstractCollection<DataMessage> newNonACLQueue) {

		this.nonACLQueue = newNonACLQueue;
	}


	//
   @Override
	public void addFirst(ACLMessage msg) {

		DataMessage dmsg;
		if( (dmsg=DataMessage.extractFromACLMessage(msg))!=null ) {
			this.nonACLQueue.add(dmsg);
		} else {
			super.addFirst(msg);
		}
	}


	//
   @Override
	public void addLast(ACLMessage msg) {

		DataMessage dmsg;
		if( (dmsg=DataMessage.extractFromACLMessage(msg))!=null ) {
			this.nonACLQueue.add(dmsg);
		} else {
			super.addLast(msg);
		}
	}

}


