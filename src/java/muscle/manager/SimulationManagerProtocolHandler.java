/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package muscle.manager;

import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import muscle.id.InstanceID;
import muscle.id.Location;
import muscle.net.ProtocolHandler;
import muscle.util.serialization.DeserializerWrapper;
import muscle.util.serialization.SerializerWrapper;

/**
 *
 * @author Joris Borgdorff
 */
public class SimulationManagerProtocolHandler extends ProtocolHandler<Boolean,SimulationManager> {
	private final static Logger logger = Logger.getLogger(SimulationManagerProtocolHandler.class.getName());

	public SimulationManagerProtocolHandler(Socket s, SimulationManager listener) {
		// Use control for in and out
		super(s, listener, true, true, 3);
	}

	@Override
	protected Boolean executeProtocol(DeserializerWrapper in, SerializerWrapper out) throws IOException {
		boolean success = false;
		in.refresh();
		SimulationManagerProtocol magic_number = SimulationManagerProtocol.valueOf(in.readInt());
		if (magic_number != SimulationManagerProtocol.MAGIC_NUMBER) {
			out.writeInt(SimulationManagerProtocol.ERROR.intValue());
			out.flush();
			socket.close();
			logger.warning("Protocol for communicating MUSCLE information is not recognized.");
			return null;
		}
		InstanceID id = null;
		int opnum = in.readInt();
		SimulationManagerProtocol proto = SimulationManagerProtocol.valueOf(opnum);
		String name = in.readString();
		if (proto != SimulationManagerProtocol.MANAGER_LOCATION) {
			id = new InstanceID(name);
		}
		switch (proto) {
			case LOCATE:
				out.writeInt(opnum);
				// Flush, to indicate that we are waiting to resolve the location
				out.flush();
				try {
					success = listener.resolve(id);
					out.writeBoolean(success);
					if (success) {
						encodeLocation(out, id.getLocation());
					}
				} catch (InterruptedException ex) {
					out.writeBoolean(false);
					logger.log(Level.SEVERE, "Could not resolve identifier", ex);
				}
				break;
			case REGISTER:
				out.writeInt(opnum);
				Location loc = decodeLocation(in);
				id.resolve(loc);
				success = listener.register(id);
				out.writeBoolean(success);
				break;
			case PROPAGATE:
				out.writeInt(opnum);
				success = listener.propagate(id);
				out.writeBoolean(success);
				break;
			case DEREGISTER:
				out.writeInt(opnum);
				success = listener.deregister(id);
				out.writeBoolean(success);
				break;
			case WILL_ACTIVATE:
				out.writeInt(opnum);
				success = listener.willActivate(id);
				out.writeBoolean(success);
				break;
			case MANAGER_LOCATION:
				out.writeInt(opnum);
				Location mgrloc = listener.getLocation();
				encodeLocation(out, mgrloc);
				success = true;
				break;
			default:
				logger.log(Level.WARNING, "Unsupported operation {0} requested", proto);
				out.writeInt(SimulationManagerProtocol.UNSUPPORTED.intValue());
				break;
		}
		out.flush();
		in.cleanUp();
		
		return success;
	}

	@Override
	public String getName() {
		return "ManagerProtocolHandler";
	}
	
}
