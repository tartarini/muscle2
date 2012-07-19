/*
 * 
 */
package muscle.client.instance;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import muscle.client.communication.Receiver;
import muscle.client.communication.message.BasicMessage;
import muscle.client.communication.message.DetachConduitSignal;
import muscle.client.communication.message.Message;
import muscle.core.ConduitDescription;
import muscle.core.ConduitExit;
import muscle.core.ConnectionScheme;
import muscle.core.DataTemplate;
import muscle.core.conduit.filter.FilterChain;
import muscle.core.kernel.InstanceController;
import muscle.core.model.Observation;
import muscle.id.InstanceID;
import muscle.id.PortalID;
import muscle.util.data.SingleProducerConsumerBlockingQueue;
import muscle.util.serialization.DataConverter;

/**
 *
 * @author Joris Borgdorff
 */
public class PassiveConduitExitController<T extends Serializable> extends PassivePortal<T> implements ConduitExitControllerImpl<T>, Receiver<T,BasicMessage,InstanceID,PortalID<InstanceID>> {
	private ConduitExit<T> conduitExit;
	private final BlockingQueue<Observation<T>> queue;
	private volatile boolean isDone;
	protected DataConverter<Message<T>,BasicMessage> converter;
	private final static Logger logger = Logger.getLogger(PassiveConduitEntranceController.class.getName());
	private final FilterChain filters;

	public PassiveConduitExitController(PortalID newPortalID, InstanceController newOwnerAgent, DataTemplate newDataTemplate) {
		super(newPortalID, newOwnerAgent, newDataTemplate);
		this.queue = new SingleProducerConsumerBlockingQueue<Observation<T>>();
		this.conduitExit = null;
		this.isDone = false;
		this.filters = createFilterChain();
	}
	
	/** Create a filter chain from the given arguments */
	private FilterChain createFilterChain() {
		ConnectionScheme cs = ConnectionScheme.getInstance();
		ConduitDescription cd = cs.exitDescriptionForPortal(portalID).getConduitDescription();
		List<String> args = cd.getArgs();
		if (args.isEmpty()) return null;
		
		FilterChain fc = new FilterChain() {
			@SuppressWarnings("unchecked")
			protected void apply(Observation subject) {
				queue.add(subject);
			}
		};
		
		fc.init(args);
		logger.log(Level.INFO, "The conduit ''{0}'' will use filter(s) {1}.", new Object[] {cd, args});
		return fc;
	}
	
	public void setExit(ConduitExit<T> exit) {
		this.conduitExit = exit;
	}

	public ConduitExit<T> getExit() {
		return this.conduitExit;
	}
	
	@Override
	public void dispose() {
		this.isDone = true;
	}

	public String toString() {
		return "pasv-in:" + this.getIdentifier();
	}

	@Override
	public BlockingQueue<Observation<T>> getMessageQueue() {
		return this.queue;
	}

	@Override
	public void messageReceived(Observation<T> obs) {
		this.setNextTimestamp(obs.getNextTimestamp());
	}

	@Override
	public void put(BasicMessage dmsg) {
		Message<T> msg = converter.deserialize(dmsg);
		if (dmsg != null) {
			if (msg.isSignal()) {
				if (msg.getSignal() instanceof DetachConduitSignal) {
					queue.add(null);
					this.dispose();
				}
			} else {
				if (this.filters == null) {
					this.queue.add(msg.getObservation());
				} else {
					this.filters.process(msg.getObservation());
				}
				increment();
			}
		}
	}

	@Override
	public void setDataConverter(DataConverter<Message<T>, BasicMessage> serializer) {
		this.converter = serializer;
	}

	@Override
	public boolean isDisposed() {
		return this.isDone;
	}
	
		@Override
	public void setComplementaryPort(PortalID<InstanceID> id) {
		// Do nothing.
	}
	
	@Override
	public Message<T> receive() {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void start() {
		// Do nothing
	}
}