package resourcemanager.system.peer.rm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
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

	private final Positive<RmPort> indexPort = positive(RmPort.class);
	private final Positive<Network> networkPort = positive(Network.class);
	private final Positive<Timer> timerPort = positive(Timer.class);
	private final Positive<CyclonSamplePort> cyclonSamplePort = positive(CyclonSamplePort.class);
	private final Positive<TManSamplePort> tmanPort = positive(TManSamplePort.class);
	private final Positive<FdetPort> fdetPort = positive(FdetPort.class);

	private final ArrayList<PeerCap> neighbours = new ArrayList<PeerCap>();

	private Address self;
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
		subscribe(handleTManSample, tmanPort);
		subscribe(handleFailure, fdetPort);
		subscribe(handleRestore, fdetPort);

		worker = create(RmWorker.class);
		connect(timerPort, worker.getNegative(Timer.class));
		subscribe(handleConfirmRequest, worker.getNegative(WorkerPort.class));
	}

	private String getId() {
		return "[" + res.getNumFreeCpus() + " " + self.getIp() + "]";
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

			// pick a random neighbour to ask for index updates from.
			// You can change this policy if you want to.
			// Maybe a gradient neighbour who is closer to the leader?
			if (neighbours.isEmpty()) {
				return;
			}
			Address dest = neighbours.get(random.nextInt(neighbours.size()))
					.getAddress();
		}
	};
	
	Handler<CyclonSample> handleCyclonSample = new Handler<CyclonSample>() {
		@Override
		public void handle(CyclonSample event) {
			// System.out.println(self.getIp() + " - received samples: "
			// + event.getSample().size());

			neighbours.clear();
			neighbours.addAll(event.getSample());
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

	public void probe(final TaskPlaceholder t) {
		assert !waiting.contains(t);
		waiting.add(t);

		FuncTools.Proposition<PeerCap> p = new FuncTools.Proposition<PeerCap>() {
			@Override
			public boolean eval(PeerCap param) {
				return param.maxMemory >= t.getMemoryInMbs()
						&& param.maxCpu >= t.getNumCpus();
			}
		};

		/* Select only among nodes which can satisfy this task */
		List<PeerCap> sel = FuncTools.filter(p, neighbours);
		Collections.shuffle(sel, random);

		List<PeerCap> chosen = new ArrayList<PeerCap>();
		int put = configuration.getProbes(); // × t.njobs
		for (PeerCap cap : sel) {
			if (put-- <= 0)
				break;
			chosen.add(cap);
			addToOustanding(cap.address, t);
			trigger(new Probing.Request(self, cap.address, t), networkPort);
		}

		log.debug(getId() + " SENDING PROBE FOR " + t.getId() + " TO " + chosen);

		// if (put > 0) {
		// log.warn("I don't know enough peers: need {} more", put);
		// }
	}

	private void serve(Address peer, Task reserved) {
		Iterator<Task> i = waiting.iterator();
		boolean isUsed = false;
		while (i.hasNext() && !isUsed) {
			Task t = i.next();
			if (reserved.getNumCpus() >= t.getNumCpus()
					&& reserved.getMemoryInMbs() >= t.getMemoryInMbs()) {
				i.remove();
				isUsed = true;
				assigned.put(t.getId(), peer);
				log.info(getId()
						+ " ASSIGNING "
						+ t.getId()
						+ " TO "
						+ peer.getIp()
						+ (t.getId() != reserved.getId() ? " (OLD:"
								+ reserved.getId() + ")" : ""));
				trigger(new Probing.Allocate(self, peer, reserved.getId(), t),
						networkPort);
			}
		}

		// if we couldn't assign anything we cancel
		// but only if that peer hasn't been already assigned for
		// that same task on a previous iteration
		if (!isUsed) {
			log.info(getId() + " SENDING CANCEL FOR " + reserved.getId()
					+ " TO " + peer.getIp());
			trigger(new Probing.Cancel(self, peer, reserved.getId()),
					networkPort);
		}
	}

	private final Handler<Probing.Response> handleProbingResponse = new Handler<Probing.Response>() {
		@Override
		public void handle(Probing.Response resp) {

			TaskPlaceholder t = getOustanding(resp.getSource(), resp.referenceId);
			log.debug(getId() + " GOT RESPONSE FROM "
					+ resp.getSource().getIp() + " FOR " + t.getId());
			if (t != null) {
				serve(resp.getSource(), t);
			}
			assert t != null;
		}

	};

	private void addToOustanding(Address address, TaskPlaceholder t) {

		if (!outstanding.containsKey(address))
			outstanding.put(address, new TreeMap<Long, TaskPlaceholder>());

		outstanding.get(address).put(t.getId(), t);

	}

	private TaskPlaceholder getOustanding(Address source, long id) {
		TaskPlaceholder toReturn = null;
		Map<Long, TaskPlaceholder> addressToIds = outstanding.get(source);
		if (addressToIds != null)
			toReturn = addressToIds.remove(id);

		if (addressToIds.isEmpty())
			outstanding.remove(source);
		return toReturn;
	}

	private final Handler<Probing.Request> handleProbingRequest = new Handler<Probing.Request>() {
		@Override
		public void handle(Probing.Request event) {

			log.debug(getId() + " GOT PROBE FOR " + event.task.getId());
			trigger(new Resources.Reserve(event.task),
					worker.getNegative(WorkerPort.class));

		}
	};

	private final Handler<Probing.Allocate> handleProbingAllocate = new Handler<Probing.Allocate>() {
		@Override
		public void handle(Probing.Allocate event) {

			log.debug(getId() + " GOT ALLOCATE FOR " + event.task.getId());
			trigger(new Resources.Allocate(event.referenceId, event.task),
					worker.getNegative(WorkerPort.class));
		}
	};

	private final Handler<Probing.Cancel> handleProbingCancel = new Handler<Probing.Cancel>() {
		@Override
		public void handle(Probing.Cancel event) {

			log.debug(getId() + " GOT cancel FOR " + event.referenceId);

			trigger(new Resources.Cancel(event.referenceId),
					worker.getNegative(WorkerPort.class));
		}
	};

	private final Handler<Resources.Confirm> handleConfirmRequest = new Handler<Resources.Confirm>() {
		@Override
		public void handle(Resources.Confirm event) {
			log.debug(getId() + " REQUESTING CONFIRMATION");
			trigger(new Probing.Response(self, event.getTask().getTaskMaster(),
					event.getTask().getId()), networkPort);

		}
	};

	Handler<ClientRequestResource> handleRequestResource = new Handler<ClientRequestResource>() {
		@Override
		public void handle(ClientRequestResource event) {

			log.debug(getId() + " allocating " + event.getId() + " resources: "
					+ event.getNumCpus() + " + " + event.getMemoryInMbs()
					+ " (" + event.getTimeToHoldResource() + ")");

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

	Handler<TManSample> handleTManSample = new Handler<TManSample>() {
		@Override
		public void handle(TManSample event) {
			// TODO:
		}
	};

}
