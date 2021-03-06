/*
* Copyright 2008, 2009 Complex Automata Simulation Technique (COAST) consortium
* Copyright 2010-2013 Multiscale Applications on European e-Infrastructures (MAPPER) project
*
* GNU Lesser General Public License
* 
* This file is part of MUSCLE (Multiscale Coupling Library and Environment).
* 
* MUSCLE is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* MUSCLE is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Lesser General Public License for more details.
* 
* You should have received a copy of the GNU Lesser General Public License
* along with MUSCLE.  If not, see <http://www.gnu.org/licenses/>.
*/
package muscle.client.communication;

import java.io.IOException;
import java.io.Serializable;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;
import muscle.client.communication.message.DetachConduitSignal;
import muscle.client.communication.message.Signal;
import muscle.client.communication.message.SignalEnum;
import muscle.core.model.Observation;
import muscle.exception.MUSCLERuntimeException;
import muscle.id.PortalID;
import muscle.id.TcpLocation;
import muscle.net.AliveSocket;
import muscle.net.SocketFactory;
import muscle.util.data.SerializableData;
import muscle.util.serialization.DataConverter;
import muscle.util.serialization.ProtocolSerializer;
import muscle.util.serialization.SerializerWrapper;

/**
 * Transmits data using TCP/IP.
 * It uses muscle.net.AliveSocket to do the communication, so that the connection
 * will remain active until a timeout occurs. At the timeout, -1 is sent over the
 * socket, this must be caught by the receiving end.
 * 
 * @author Joris Borgdorff
 * @param <T> Datatype that will be serialized
 */
public class TcpTransmitter<T extends Serializable> extends Transmitter<T, SerializableData> {
	private final AliveSocket liveSocket;
	private final static Logger logger = Logger.getLogger(TcpTransmitter.class.getName());
	private final static long socketKeepAlive = 5000*1000;
	private final SocketFactory socketFactory;
	private final static ProtocolSerializer<TcpDataProtocol> protocol = TcpDataProtocol.handler;
	
	public TcpTransmitter(SocketFactory sf, DataConverter<Observation<T>, Observation<SerializableData>> converter, PortalID portalID) {
		super(converter, portalID);
		this.socketFactory = sf;
		this.liveSocket = new AliveSocket(socketFactory, ((TcpLocation)portalID.getLocation()).getSocketAddress(), socketKeepAlive);
	}
	
	public void start() {
		this.liveSocket.start();
	}
	
	// Synchronized: can only transmit one signal or message at a time.
	@Override
	public synchronized void transmit(Observation<T> obs) {
		if (converter == null) {
			throw new IllegalStateException("Can not send message without serialization");
		}
		if (this.isDisposed()) {
			logger.log(Level.WARNING, "Transmitter is disposed of; unable to send observation to {0}", portalID);
			return;
		}
		send(converter.serialize(obs), null);
	}

	// Synchronized: can only transmit one signal or message at a time.
	@Override
	public synchronized void signal(Signal signal) {
		if (this.isDisposed()) {
			if (!(signal instanceof DetachConduitSignal)) {
				logger.log(Level.WARNING, "Transmitter is disposed of; unable to send signal {0} to {1}", new Object[] {signal, portalID});
			}

			return;
		}
		send(null, signal);
	}
	
	private void send(Observation<SerializableData> obs, Signal signal) {
		boolean sent = false;
		for (int tries = 1; !sent && tries <= 4; tries++) {
			if (liveSocket.lock()) {
				try {
					SerializerWrapper out = liveSocket.getOutput();

					protocol.write(out, TcpDataProtocol.MAGIC_NUMBER);
					if (obs != null) {
						sendMessage(out, obs);
					}
					else if (signal != null) {
						sendSignal(out, signal);
					}
					out.flush();
					sent = true;
					if (tries > 1) {
						logger.log(Level.INFO, "Sending message to {0} succeeded at try {1}", new Object[]{portalID, tries});
					}
				} catch (MUSCLERuntimeException ex) {
					if (isDisposed()) {
						break;
					} else {
						throw ex;
					}
				} catch (SocketException ex) {
					if (isDisposed()) {
						break;
					}
					logger.log(Level.SEVERE, "Message not sent: socket was closed by " + portalID + ".", ex);
					throw new MUSCLERuntimeException(ex);
				} catch (IOException ex) {
					if (isDisposed()) {
						break;
					}
					String t = sent ? "" : "; tried " + tries + "/4 times.";
					if (signal == null) {
						logger.log(Level.WARNING, "Failed to send message to " + portalID + t, ex);
					} else {
						logger.log(Level.SEVERE, "Failure to send signal " + signal + " to " + portalID + t, ex);
					}
					if (tries == 4) {
						throw new MUSCLERuntimeException(ex);
					} else if (tries > 1) {
						try {
							// Sleep longer and longer
							Thread.sleep(1000*(55*(tries - 2) + 5));
						} catch (InterruptedException ex1) {}
					}
				} finally {
					liveSocket.unlock();
				}
			} else {
				logger.log(Level.WARNING, "Can not send message: connection to {0} already closed.", portalID);
				break;
			}
		}
	}
	
	private void sendMessage(SerializerWrapper out, Observation<SerializableData> obs) throws IOException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.log(Level.FINEST, "Sending data message of type {0} to {1}", new Object[] {obs.getData().getType(), portalID});
		}
		protocol.write(out, TcpDataProtocol.OBSERVATION);
		out.writeString(portalID.getName());
		out.writeInt(portalID.getType().ordinal());
		out.writeDouble(obs.getTimestamp().doubleValue());
		out.writeDouble(obs.getNextTimestamp().doubleValue());
		obs.getData().encodeData(out);
	}
	
	private void sendSignal(SerializerWrapper out, Signal signal) throws IOException {
		SignalEnum sig;
		if (signal instanceof DetachConduitSignal) {
			sig = SignalEnum.DETACH_CONDUIT;
		} else {
			return;
		}
		logger.log(Level.FINEST, "Sending signal {0} to {1}", new Object[] {sig, portalID});

		protocol.write(out, TcpDataProtocol.SIGNAL);
		out.writeString(portalID.getName());
		out.writeInt(portalID.getType().ordinal());
		out.writeInt(sig.ordinal());
	}
	
	@Override
	public synchronized void dispose() {
		super.dispose();
		liveSocket.dispose();
	}
}
