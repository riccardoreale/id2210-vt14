package resourcemanager.system.peer.rm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import common.utils.FuncTools;
import common.utils.FuncTools.Proposition;
import common.utils.SoftMax;

import cyclon.system.peer.cyclon.CyclonSample;
import cyclon.system.peer.cyclon.CyclonSamplePort;
import cyclon.system.peer.cyclon.PeerDescriptor;

/**
 * Should have some comments here.
 * 
 * @author jdowling
 */
public final class ResourceManager extends ComponentDefinition {

	public class ObjectId {

		@Override
		public String toString() {
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("[");
			stringBuilder.append(res.getNumFreeCpus());
			stringBuilder.append(" ");
			stringBuilder.append(self.getIp());
			stringBuilder.append("]");
			return stringBuilder.toString();
		}

	}

	private static final Logger log = LoggerFactory
			.getLogger(ResourceManager.class);

	public static double TEMPERATURE = 0.5;
	public static boolean USE_GRADIENT = true;
	public static int MAX_HOPS = 5;

	private final Positive<RmPort> indexPort = positive(RmPort.class);
	private final Positive<Network> networkPort = positive(Network.class);
	private final Positive<Timer> timerPort = positive(Timer.class);
	private final Positive<CyclonSamplePort> cyclonSamplePort = positive(CyclonSamplePort.class);
	private final Positive<TManSamplePort> tmanPort = positive(TManSamplePort.class);

	private final ArrayList<PeerCap> neighbours = new ArrayList<PeerCap>();
	private final Set<Long> probes = new HashSet<Long>();

	private Address self;
	private ObjectId selfId = new ObjectId();
	private RmConfiguration configuration;
	private Random random;
	private Component worker;
	private AvailableResourcesImpl res;

	/* Components for probing */
	private final Queue<Task> waiting = new LinkedList<Task>();
	private final Map<Long, Address> assigned = new HashMap<Long, Address>();
	private final Map<Address, TreeMap<Long, TaskPlaceholder>> outstanding = new TreeMap<Address, TreeMap<Long, TaskPlaceholder>>();

	// private AvailableResources availableResources;
	// private TaskQueue queue;
	// When you partition the index you need to find new nodes
	// This is a routing table maintaining a list of pairs in each partition.
	private Map<Integer, List<PeerDescriptor>> routingTable;
	Comparator<PeerDescriptor> peerAgeComparator = new Comparator<PeerDescriptor>() {
		@Override
		public int compare(PeerDescriptor t, PeerDescriptor t1) {
			if (t.getAge() > t1.getAge()) {
				return 1;
			} else {
				return -1;
			}
		}
	};

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
		subscribe(handleTManSample, tmanPort);

		worker = create(RmWorker.class);
		connect(timerPort, worker.getNegative(Timer.class));
		subscribe(handleConfirmRequest, worker.getNegative(WorkerPort.class));
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

	public void probe(final TaskPlaceholder t) {
		assert !waiting.contains(t);
		waiting.add(t);

		if (res.isAvailable(t.getNumCpus(), t.getMemoryInMbs())) {

			trigger(new Probing.Request(self, self, t, self, 10), networkPort);
			log.debug("{} SENDING PROBE FOR {} TO  MYSELF", getId(), t.getId());

		} else {
			// number of probes
			int put = configuration.getProbes();

			List<PeerCap> chosen = new ArrayList<PeerCap>();
			List<Address> chosenAddress = new ArrayList<Address>();

			while (put-- > 0) {

				PeerCap cap = selectNextHop(t, chosenAddress);
				if (cap == null)
					break;

				chosen.add(cap);
				chosenAddress.add(cap.getAddress());

				trigger(new Probing.Request(self, cap.address, t, self, 0),
						networkPort);
			}

			log.debug("{} SENDING PROBE FOR {} TO {}", new Object[] { getId(),
					t.getId(), chosen });
		}

	}

	private PeerCap selectNextHop(final TaskPlaceholder t,
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
			SoftMax softMax = new SoftMax(sel, TEMPERATURE, random);
			return softMax.pickPeer();
		}
	}

	private void serve(Address peer, Task reserved) {
		Iterator<Task> i = waiting.iterator();
		boolean isUsed = false;
		/*
		 * we check if we have any pending task to allocate to this available
		 * peer
		 */
		while (i.hasNext() && !isUsed) {
			Task t = i.next();
			if (reserved.getNumCpus() >= t.getNumCpus()
					&& reserved.getMemoryInMbs() >= t.getMemoryInMbs()) {
				i.remove();
				isUsed = true;
				assigned.put(t.getId(), peer);

				log.debug(
						"{} ASSIGNING {} TO {} {}",
						new Object[] {
								getId(),
								t.getId(),
								peer.getIp(),
								(t.getId() != reserved.getId() ? " (OLD:"
										+ reserved.getId() + ")" : "") });
				trigger(new Probing.Allocate(self, peer, reserved.getId(), t),
						networkPort);
			}
		}

		/*
		 * if we couldn't assign anything we cancel it TODO: maybe proactive
		 * cancellation can improve gradient
		 */
		if (!isUsed) {
			log.debug("{} SENDING CANCEL FOR {} TO {}", new Object[] { getId(),
					reserved.getId(), peer.getIp() });
			trigger(new Probing.Cancel(self, peer, reserved.getId()),
					networkPort);
		}
	}

	private void addToOustanding(Address address, TaskPlaceholder t) {

		if (!outstanding.containsKey(address))
			outstanding.put(address, new TreeMap<Long, TaskPlaceholder>());

		outstanding.get(address).put(t.getId(), t);

	}

	private TaskPlaceholder getOustanding(Address source, long id) {
		TaskPlaceholder toReturn = null;
		Map<Long, TaskPlaceholder> addressToIds = outstanding.get(source);
		if (addressToIds == null)
			return null;
		if (addressToIds != null)
			toReturn = addressToIds.remove(id);

		if (addressToIds.isEmpty())
			outstanding.remove(source);
		return toReturn;
	}

	private final Handler<Probing.Request> handleProbingRequest = new Handler<Probing.Request>() {
		@Override
		public void handle(Probing.Request event) {

			boolean depositProbe = true;

			/*
			 * check if we can accept it, or reached the max number of
			 * propagations or if we already got it
			 */
			if (!res.isAvailable(event.task.getNumCpus(),
					event.task.getMemoryInMbs())
					&& event.count < MAX_HOPS
					|| probes.contains(event.task.getId())) {

				ArrayList<Address> exclude = new ArrayList<Address>();
				exclude.add(event.getSource());
				PeerCap dest = selectNextHop(event.task, exclude);

				if (dest != null) {
					log.debug("{} PROPAGATING PROBE FOR {}", getId(),
							event.task.getId());
					trigger(new Probing.Request(self, dest.getAddress(),
							event.task, event.taskMaster, event.count + 1),
							networkPort);
					depositProbe = false;
				}
			}

			/*
			 * I deposit the probe and reserve resources on the worker and
			 * communicate back to the master
			 */
			if (depositProbe) {
				log.debug("{} GOT PROBE FOR {}", getId(), event.task.getId());
				probes.add(event.task.getId());
				trigger(new Probing.GotRequest(self, event.taskMaster,
						event.task), networkPort);
				trigger(new Resources.Reserve(event.task),
						worker.getNegative(WorkerPort.class));
			}
		}
	};

	private final Handler<Probing.GotRequest> handleGotProbeRequest = new Handler<Probing.GotRequest>() {
		@Override
		public void handle(Probing.GotRequest resp) {

			addToOustanding(resp.getSource(), resp.task);
		}

	};

	private final Handler<Probing.Response> handleProbingResponse = new Handler<Probing.Response>() {
		@Override
		public void handle(Probing.Response resp) {

			TaskPlaceholder t = getOustanding(resp.getSource(), resp.id);

			assert t != null : getId() + " NOT FOUND "
					+ resp.getSource().getIp() + " FOR " + resp.id;

			log.debug("{} GOT RESPONSE FROM {} FOR {}", new Object[] { getId(),
					resp.getSource().getIp(), t.getId() });
			serve(resp.getSource(), t);
		}

	};

	private final Handler<Probing.Allocate> handleProbingAllocate = new Handler<Probing.Allocate>() {
		@Override
		public void handle(Probing.Allocate event) {

			log.debug("{} GOT ALLOCATE FOR {}", getId(), event.task.getId());
			trigger(new Resources.Allocate(event.referenceId, event.task),
					worker.getNegative(WorkerPort.class));
		}
	};

	private final Handler<Probing.Cancel> handleProbingCancel = new Handler<Probing.Cancel>() {
		@Override
		public void handle(Probing.Cancel event) {

			log.debug("{} GOT cancel FOR {}", getId(), event.refId);

			trigger(new Resources.Cancel(event.refId),
					worker.getNegative(WorkerPort.class));
		}
	};

	private final Handler<Resources.Confirm> handleConfirmRequest = new Handler<Resources.Confirm>() {
		@Override
		public void handle(Resources.Confirm event) {
			log.debug("{} REQUESTING CONFIRMATION FOR {}", getId(), event
					.getTask().getId());
			trigger(new Probing.Response(self, event.getTask().getTaskMaster(),
					event.getTask().getId()), networkPort);

		}
	};

	Handler<ClientRequestResource> handleRequestResource = new Handler<ClientRequestResource>() {
		@Override
		public void handle(ClientRequestResource event) {

			log.info(
					"{} got ClientRequestResource {} resources: {} + {} ({})",
					new Object[] { getId(), event.getId(), event.getNumCpus(),
							event.getMemoryInMbs(),
							event.getTimeToHoldResource() });

			/*
			 * // Direct allocation trigger(new AllocateResources(event.getId(),
			 * event.getNumCpus(), event.getMemoryInMbs(),
			 * event.getTimeToHoldResource()),
			 * worker.getNegative(WorkerPort.class));
			 */
			TaskPlaceholder t = new TaskPlaceholder(event.getId(),
					event.getNumCpus(), event.getMemoryInMbs(),
					event.getTimeToHoldResource(), self);

			t.queue();

			if (configuration.isOmniscent()) {
				t.executeDirectly = true;
				trigger(new Resources.Reserve(t),
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
}
