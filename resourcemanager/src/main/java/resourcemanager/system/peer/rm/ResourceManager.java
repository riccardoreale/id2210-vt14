package resourcemanager.system.peer.rm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import resourcemanager.system.peer.rm.Probing.Completed;
import resourcemanager.system.peer.rm.task.AvailableResourcesImpl;
import resourcemanager.system.peer.rm.task.RmWorker;
import resourcemanager.system.peer.rm.task.Task;
import resourcemanager.system.peer.rm.task.TaskPlaceholder;
import resourcemanager.system.peer.rm.task.WorkerInit;
import resourcemanager.system.peer.rm.task.WorkerPort;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timer;
import system.peer.RmPort;
import tman.system.peer.tman.TManSample;
import tman.system.peer.tman.TManSamplePort;
import common.configuration.RmConfiguration;
import common.peer.PeerCap;
import common.simulation.ClientRequestResource;
import common.simulation.TaskResources;
import common.utils.FuncTools;
import common.utils.FuncTools.Proposition;
import common.utils.SoftMax;
import cyclon.system.peer.cyclon.CyclonSample;
import cyclon.system.peer.cyclon.CyclonSamplePort;
import cyclon.system.peer.cyclon.PeerDescriptor;
import fdet.system.evts.FdetPort;

/**
 * Should have some comments here.
 * 
 * @author jdowling
 */
public final class ResourceManager extends ComponentDefinition {

	private static final Logger log = LoggerFactory
			.getLogger(ResourceManager.class);

	/**
	 * the temperature for the SoftMax process that selects the next hop to send
	 * a probe to.<br>
	 * <br>
	 * A value of 0 means GREEDY, it will select the peer that reports the
	 * highest utility function.<br>
	 * A value >> 0 (e.g. 100) will select a random peer
	 */
	public static double TEMPERATURE_PROBING = 1;

	/**
	 * if <b>false</b> will use the Cyclon random view to select peer for probes <br>
	 * if <b>true</b> will use the Gradient view
	 */
	public static boolean USE_GRADIENT = false;

	/**
	 * maximum number of HOPS a probe can be propagated
	 */
	public static int MAX_HOPS = 5;

	private final Positive<RmPort> indexPort = positive(RmPort.class);
	private final Positive<Network> networkPort = positive(Network.class);
	private final Positive<Timer> timerPort = positive(Timer.class);
	private final Positive<CyclonSamplePort> cyclonSamplePort = positive(CyclonSamplePort.class);
	private final Positive<TManSamplePort> tmanPort = positive(TManSamplePort.class);
	private final Positive<FdetPort> fdetPort = positive(FdetPort.class);

	private final ArrayList<PeerCap> neighbours = new ArrayList<PeerCap>();
	private final Set<Long> depositedProbes = new HashSet<Long>();

	private Address self;
	private ObjectId selfId = new ObjectId();
	private RmConfiguration configuration;
	private Random random;
	private Component worker;
	private AvailableResourcesImpl res;

	/* Components for probing */
	private final Queue<Task> waiting = new LinkedList<Task>();
	private final Map<Long, Address> assigned = new HashMap<Long, Address>();
	private final Map<Long, Outstanding> outstanding = new HashMap<Long, ResourceManager.Outstanding>();

	public ResourceManager() {

		subscribe(handleInit, control);
		subscribe(handleCyclonSample, cyclonSamplePort);
		subscribe(handleRequestResource, indexPort);
		subscribe(handleUpdateTimeout, timerPort);
		subscribe(handleProbingRequest, networkPort);
		subscribe(handleProbingResponse, networkPort);
		subscribe(handleProbingAllocate, networkPort);
		subscribe(handleProbingCancel, networkPort);
		subscribe(handleGotProbeRequest, networkPort);
		subscribe(handleProbingTerminate, networkPort);
		subscribe(handleTManSample, tmanPort);
		subscribe(handleFailure, fdetPort);
		subscribe(handleRestore, fdetPort);

		worker = create(RmWorker.class);
		connect(timerPort, worker.getNegative(Timer.class));
		subscribe(handleConfirmRequest, worker.getNegative(WorkerPort.class));
		subscribe(handleCompleted, worker.getNegative(WorkerPort.class));
	}

	private ObjectId getId() {
		return selfId;
	}

	Handler<RmInit> handleInit = new Handler<RmInit>() {

		@Override
		public void handle(RmInit init) {
			self = init.getSelf();
			configuration = init.getConfiguration();
			random = new Random(init.getConfiguration().getSeed()
					* self.getId());
			long period = configuration.getPeriod();

			SchedulePeriodicTimeout rst = new SchedulePeriodicTimeout(period,
					period);
			rst.setTimeoutEvent(new UpdateTimeout(rst));
			trigger(rst, timerPort);

			res = (AvailableResourcesImpl) init.getAvailableResources();
			trigger(new WorkerInit(self, res), worker.getControl());

		}
	};

	Handler<UpdateTimeout> handleUpdateTimeout = new Handler<UpdateTimeout>() {
		@Override
		public void handle(UpdateTimeout event) {

			if (neighbours.isEmpty()) {
				return;
			}
			Address dest = neighbours.get(random.nextInt(neighbours.size()))
					.getAddress();
		}
	};
	
	Handler<FdetPort.Dead> handleFailure = new Handler<FdetPort.Dead>() {
		@Override
		public void handle(FdetPort.Dead event) {
		}
	};

	Handler<FdetPort.Undead> handleRestore = new Handler<FdetPort.Undead>() {
		@Override
		public void handle(FdetPort.Undead event) {
		}
	};

	public void probe(final Task t) {
		assert !waiting.contains(t);
		waiting.add(t);
		outstanding.put(t.id, new Outstanding(t));

		// first it checks if it can execute a task itself
		if (res.isAvailable(t.required.numCpus, t.required.memoryInMbs)) {

			trigger(new Probing.Request(self, self, t.required, t.id, self, 10), networkPort);
			
			log.debug("{} SENDING PROBE FOR {} TO  MYSELF", getId(), t.id);

		} else {
			// otherwise it sends N new probes to selected peers

			int put = configuration.getProbes();
			List<PeerCap> chosen = new ArrayList<PeerCap>();
			List<Address> chosenAddress = new ArrayList<Address>();

			while (put-- > 0) {

				// we select the next hop excluding the already selected
				// addresses
				PeerCap cap = selectNextHop(t.required, chosenAddress);
				if (cap == null)
					break;

				chosen.add(cap);
				chosenAddress.add(cap.getAddress());

				trigger(new Probing.Request(self, cap.address, t.required, t.id, self, 0),
						networkPort);
			}

			log.debug("{} SENDING PROBE FOR {} TO {}", new Object[] { getId(),
					t.id, chosen });
		}

	}

	/**
	 * filters out excluded peers from the neighbour view and uses SoftMax to
	 * pick the best peer (based on the Temperature)
	 * 
	 * @param t
	 * @param exclude
	 *            list of peers to exclude
	 * @return the selected node
	 */
	private PeerCap selectNextHop(final TaskResources t,
			final List<Address> exclude) {

		Proposition<PeerCap> p = new Proposition<PeerCap>() {
			@Override
			public boolean eval(PeerCap param) {
				if (exclude.contains(param.getAddress()))
					return false;

				return true;
			}
		};

		/* Select only among nodes not excluded */
		List<PeerCap> sel = FuncTools.filter(p, neighbours);

		if (sel.size() == 0)
			return null;
		else if (sel.size() == 1)
			return sel.get(0);
		else {
			/*
			 * based on temperature we can return greedy (best utility) or
			 * completely random
			 */
			SoftMax softMax = new SoftMax(sel, TEMPERATURE_PROBING, random);
			return softMax.pickPeer();
		}
	}

	/**
	 * called when a peer request a confirmation and therefore has available
	 * resources.<br>
	 * this method will send a task allocation for the first pending task that
	 * matches the available resources on that node.<br>
	 * 
	 * If there are no pending task it sends a cancel to release the resources
	 * on that node
	 * 
	 * @param peer
	 * @param reserved
	 */
	private void serve(Address peer, Task reserved) {
		Iterator<Task> i = waiting.iterator();
		boolean isUsed = false;
		/*
		 * we check if we have any pending task to allocate to this available
		 * peer
		 */
		while (i.hasNext() && !isUsed) {
			Task t = i.next();
			if (reserved.required.numCpus >= t.required.numCpus
					&& reserved.required.memoryInMbs >= t.required.memoryInMbs) {
				i.remove();
				isUsed = true;
				assigned.put(t.id, peer);

				log.debug(
						"{} ASSIGNING {} TO {} {}",
						new Object[] {
								getId(),
								t.getId(),
								peer.getIp(),
								(t.id != reserved.getId() ? " (OLD:"
										+ reserved.getId() + ")" : "") });
				trigger(new Probing.Allocate(self, peer, reserved.getId(), t),
						networkPort);
			}
		}

		/*
		 * if we couldn't assign anything we cancel it
		 */
		if (!isUsed) {
			log.debug("{} SENDING CANCEL FOR {} TO {}", new Object[] { getId(),
					reserved.getId(), peer.getIp() });
			trigger(new Probing.Cancel(self, peer, reserved.getId()),
					networkPort);
		}
	}

	private Task getOustanding(Address source, long id) {
		Outstanding outRef = outstanding.get(id);
		if(outRef == null)
			return null;
		
		assert outRef.subscribers.contains(source) : "PEER NOT SUBSCRIBED";
			
		return outRef.t;
	}

	/**
	 * handling a probe<br>
	 * - checks if it has resources to execute the task immediately<br>
	 * - checks if the probe has reached the max hops<br>
	 * - checks if it has already accepted a probe for the same task <br>
	 * <br>
	 * If it can't deposit, it selects one single next peer and propagates the
	 * probe to it <br>
	 * if it can deposit, it sends the task down to his own Worker and send a
	 * GotRequest to the master to notify him that the task his added in his
	 * waiting queue
	 */
	private final Handler<Probing.Request> handleProbingRequest = new Handler<Probing.Request>() {
		@Override
		public void handle(Probing.Request event) {

			boolean depositProbe = true;

			boolean available = res.isAvailable(event.required.getNumCpus(),
					event.required.getMemoryInMbs());
			boolean alreadyGot = depositedProbes.contains(event.taskId);
			
			if (!available && event.count < MAX_HOPS || alreadyGot) {

				ArrayList<Address> exclude = new ArrayList<Address>();
				exclude.add(event.getSource());
				PeerCap dest = selectNextHop(event.required, exclude);

				if (dest != null) {
					log.debug("{} PROPAGATING PROBE FOR {}", getId(),
							event.taskId);
					trigger(new Probing.Request(self, dest.getAddress(), event.required,
							event.taskId, event.taskMaster, event.count + 1),
							networkPort);
					depositProbe = false;
				}
			}

			/*
			 * I deposit the probe and reserve resources on the worker and
			 * communicate back to the master
			 */
			if (depositProbe) {
				log.debug("{} GOT PROBE FOR {} AFTER {} HOPS ({})",
						new Object[] { getId(), event.taskId,
								event.count, available });
				depositedProbes.add(event.taskId);
				trigger(new Probing.GotRequest(self, event.taskMaster,
						event.taskId), networkPort);
				trigger(new Resources.Reserve(event.taskMaster, event.taskId, event.required),
						worker.getNegative(WorkerPort.class));
			}
		}
	};

	/**
	 * a task has been "accepted" and put in the queue by another node
	 */
	private final Handler<Probing.GotRequest> handleGotProbeRequest = new Handler<Probing.GotRequest>() {
		@Override
		public void handle(Probing.GotRequest resp) {

			Outstanding outRef = outstanding.get(resp.referenceId);
			outRef.subscribers.add(resp.getSource());
			
		}

	};
	

	/**
	 * a task has been selected and resources has been allocated. The master
	 * need to confirm the task, send another pending task with same of less
	 * resource, or release the resources.
	 */
	private final Handler<Probing.Response> handleProbingResponse = new Handler<Probing.Response>() {
		@Override
		public void handle(Probing.Response resp) {

			Task t = getOustanding(resp.getSource(), resp.referenceId);

			assert t != null : getId() + " NOT FOUND "
					+ resp.getSource().getIp() + " FOR " + resp.referenceId;

			log.debug("{} GOT RESPONSE FROM {} FOR {}", new Object[] { getId(),
					resp.getSource().getIp(), t.getId() });
			serve(resp.getSource(), t);
		}

	};

	/**
	 * got allocation for executing a task immediately
	 */
	private final Handler<Probing.Allocate> handleProbingAllocate = new Handler<Probing.Allocate>() {
		@Override
		public void handle(Probing.Allocate event) {

			log.debug("{} GOT ALLOCATE FOR {}", getId(), event.task.id);
			trigger(new Resources.Allocate(event.getSource(), event.referenceId, event.task),
					worker.getNegative(WorkerPort.class));
		}
	};

	/**
	 * handling cancel of a probe
	 */
	private final Handler<Probing.Cancel> handleProbingCancel = new Handler<Probing.Cancel>() {
		@Override
		public void handle(Probing.Cancel event) {

			log.debug("{} GOT cancel FOR {}", getId(), event.referenceId);

			trigger(new Resources.Cancel(event.referenceId),
					worker.getNegative(WorkerPort.class));
		}
	};

	private final Handler<Resources.Confirm> handleConfirmRequest = new Handler<Resources.Confirm>() {
		@Override
		public void handle(Resources.Confirm event) {
			log.debug("{} REQUESTING CONFIRMATION FOR {}", getId(), event
					.tph.getId());
			trigger(new Probing.Response(self, event.tph.taskMaster,
					event.tph.getId()), networkPort);
		}
	};
	
	private final Handler<Resources.Completed> handleCompleted = new Handler<Resources.Completed>() {
		@Override
		public void handle(Resources.Completed event) {
			log.debug(getId() + " SIGNALING TERMINATION TO " + event.taskMaster);
			trigger(new Probing.Completed(self, event.taskMaster, event.task.id), networkPort);
		}
	};

	private final Handler<Probing.Completed> handleProbingTerminate = new Handler<Probing.Completed>() {
		@Override
		public void handle(Completed event) {
			log.debug(getId() + " TASK TERMINATED BY " + event.getSource());
		}
	};

	Handler<ClientRequestResource> handleRequestResource = new Handler<ClientRequestResource>() {
		@Override
		public void handle(ClientRequestResource event) {

			log.info(
					"{} got ClientRequestResource {} resources: {} + {} ({})",
					new Object[] { getId(), event.taskId, event.required.getNumCpus(),
							event.required.getMemoryInMbs(),
							event.timeToHoldResource });

			/*
			 * // Direct allocation trigger(new AllocateResources(event.getId(),
			 * event.getNumCpus(), event.getMemoryInMbs(),
			 * event.getTimeToHoldResource()),
			 * worker.getNegative(WorkerPort.class));
			 */
			Task t = new Task(event.taskId, event.required,
					event.timeToHoldResource);
			t.queue();

			if (configuration.isOmniscent()) {
				trigger(new Resources.AllocateDirectly(self, t),
						worker.getNegative(WorkerPort.class));
			} else {
				probe(t);
			}
		}
	};

	Handler<CyclonSample> handleCyclonSample = new Handler<CyclonSample>() {
		@Override
		public void handle(CyclonSample event) {
			// receive a new list of neighbours
			printGradient(event.getSample(), false);
			if (!USE_GRADIENT) {
				neighbours.clear();
				neighbours.addAll(event.getSample());
				// printGradient();
			}
		}
	};

	Handler<TManSample> handleTManSample = new Handler<TManSample>() {
		@Override
		public void handle(TManSample event) {
			printGradient(event.getSample(), true);
			if (USE_GRADIENT) {
				neighbours.clear();
				neighbours.addAll(event.getSample());
			}
		}
	};

	private void printGradient(ArrayList<PeerCap> list, boolean gradient) {
		double avgDistance = 0;
		double totDistance = 0;
		for (PeerCap p : list)
			totDistance = Math.abs(res.getNumFreeCpus() - p.getAvailableCpu());

		avgDistance = (double) totDistance / list.size();
		// if (gradient)
		// System.err.println(System.currentTimeMillis() + "\t"
		// + res.getNumFreeCpus() + "\t\t" + avgDistance + "\t"
		// + list.size());
		// else
		// System.err.println(System.currentTimeMillis() + "\t"
		// + res.getNumFreeCpus() + "\t" + avgDistance + "\t\t"
		// + list.size());
	}

	public class ObjectId {

		@Override
		public String toString() {
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("[");
			stringBuilder.append(res.getNumFreeCpus() - res.getQueueLength());
			stringBuilder.append(" ");
			stringBuilder.append(self.getIp());
			stringBuilder.append("]");
			return stringBuilder.toString();
		}
	}
	
	public class Outstanding {
		final Task t;
		List<Address> subscribers = new ArrayList<Address>();
		
		public Outstanding(Task t) {
			this.t = t;
		}
	}
}
