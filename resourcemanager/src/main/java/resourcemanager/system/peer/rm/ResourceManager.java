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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import resourcemanager.system.peer.rm.task.AvailableResourcesImpl;
import resourcemanager.system.peer.rm.task.RmWorker;
import resourcemanager.system.peer.rm.task.WorkerInit;
import resourcemanager.system.peer.rm.task.WorkerPort;
import resourcemanager.system.peer.rm.task.Task;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.web.Web;
import system.peer.RmPort;
import tman.system.peer.tman.TManSample;
import tman.system.peer.tman.TManSamplePort;

import common.configuration.RmConfiguration;
import common.peer.PeerCap;
import common.simulation.ClientRequestResource;
import common.utils.FuncTools;
import common.utils.FuncTools.Proposition;

import cyclon.system.peer.cyclon.CyclonSample;
import cyclon.system.peer.cyclon.CyclonSamplePort;
import cyclon.system.peer.cyclon.PeerDescriptor;

/**
 * Should have some comments here.
 * 
 * @author jdowling
 */
public final class ResourceManager extends ComponentDefinition {

	private static final int PROBES_PER_JOB = 2;

	private static final Logger logger = LoggerFactory
			.getLogger(ResourceManager.class);

	private final Positive<RmPort> indexPort = positive(RmPort.class);
	private final Positive<Network> networkPort = positive(Network.class);
	private final Positive<Timer> timerPort = positive(Timer.class);
	private final Negative<Web> webPort = negative(Web.class);
	private final Positive<CyclonSamplePort> cyclonSamplePort = positive(CyclonSamplePort.class);
	private final Positive<TManSamplePort> tmanPort = positive(TManSamplePort.class);
	private final ArrayList<PeerCap> neighbours = new ArrayList<PeerCap>();
	private Address self;
	private RmConfiguration configuration;
	private Random random;
	private Component worker;

	/* Components for probing */
	private final Queue<Task> waiting = new LinkedList<Task>();
	private final Map<Address, PeerCap> outstanding = new HashMap<Address, PeerCap>();

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
		subscribe(handleResourceAllocationRequest, networkPort);
		subscribe(handleResourceAllocationResponse, networkPort);
		subscribe(handleProbingRequest, networkPort);
		subscribe(handleProbingResponse, networkPort);
		subscribe(handleTManSample, tmanPort);

		worker = create(RmWorker.class);
		connect(timerPort, worker.getNegative(Timer.class));
	}

	Handler<RmInit> handleInit = new Handler<RmInit>() {
		@Override
		public void handle(RmInit init) {

			self = init.getSelf();
			configuration = init.getConfiguration();
			random = new Random(init.getConfiguration().getSeed());
			long period = configuration.getPeriod();

			SchedulePeriodicTimeout rst = new SchedulePeriodicTimeout(period,
					period);
			rst.setTimeoutEvent(new UpdateTimeout(rst));
			trigger(rst, timerPort);

			trigger(new WorkerInit(self,
					(AvailableResourcesImpl) init.getAvailableResources()),
					worker.getControl());

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

	Handler<RequestResources.Request> handleResourceAllocationRequest = new Handler<RequestResources.Request>() {
		@Override
		public void handle(RequestResources.Request event) {
			System.out
					.println(self.getIp() + " - GOT RequestResources.Request");

		}
	};

	Handler<RequestResources.Response> handleResourceAllocationResponse = new Handler<RequestResources.Response>() {
		@Override
		public void handle(RequestResources.Response event) {
			// TODO
		}
	};

	Handler<CyclonSample> handleCyclonSample = new Handler<CyclonSample>() {
		@Override
		public void handle(CyclonSample event) {
			System.out.println(self.getIp() + " - received samples: "
					+ event.getSample().size());

			// receive a new list of neighbours
			neighbours.clear();
			// TODO: addAll when cyclon is fixed
			// neighbours.addAll(event.getSample());

		}
	};

	public void probe(final Task t) {
		assert !waiting.contains(t);
		waiting.add(t);

		Proposition<PeerCap> p = new Proposition<PeerCap>() {
			@Override
			public boolean eval(PeerCap param) {
				return param.maxMemory >= t.getMemoryInMbs()
						&& param.maxCpu >= t.getNumCpus()
						&& !outstanding.containsKey(param.address);
			}
		};

		/* Select only among nodes which can satisfy this task */
		List<PeerCap> sel = FuncTools.filter(p, neighbours);
		Collections.shuffle(sel);

		int put = PROBES_PER_JOB; // × t.njobs
		for (PeerCap cap : sel) {
			if (put-- <= 0)
				break;

			outstanding.put(cap.address, cap);
			trigger(new Probing.Request(self, cap.address), networkPort);
		}

		if (put > 0) {
			logger.warn("I don't know enough peers: need {} more", put);
		}
	}

	private void serve(Address peer, PeerCap cap) {
		Iterator<Task> i = waiting.iterator();
		boolean assigned = false;
		while (i.hasNext() && !assigned) {
			Task t = i.next();
			if (cap.canRun(t.getNumCpus(), t.getMemoryInMbs())) {
				i.remove();
				assigned = true;
			}
		}

		System.err.printf("serve(%s) -> assigned? %s\n", peer, assigned);

		/* No waiting tasks left? Send cancellation */
		if (waiting.isEmpty()) {
			trigger(new Probing.Cancel(self, peer), networkPort);
			for (Address to : outstanding.keySet()) {
				trigger(new Probing.Cancel(self, to), networkPort);
			}
		}
	}

	private final Handler<Probing.Response> handleProbingResponse = new Handler<Probing.Response>() {
		@Override
		public void handle(Probing.Response resp) {
			Address peer = resp.getSource();
			PeerCap cap = outstanding.remove(peer);
			if (cap != null) {
				serve(peer, cap);
			}
			assert false : "Got orphan of a dead node";
		}
	};

	private final Handler<Probing.Request> handleProbingRequest = new Handler<Probing.Request>() {
		@Override
		public void handle(Probing.Request event) {
			assert false : "ok, we got";
		}
	};

	Handler<ClientRequestResource> handleRequestResource = new Handler<ClientRequestResource>() {
		@Override
		public void handle(ClientRequestResource event) {

			System.out.println(self.getIp() + " allocating resources: "
					+ event.getNumCpus() + " + " + event.getMemoryInMbs()
					+ " (" + event.getTimeToHoldResource() + ")");
			// TODO: Ask for resources from neighbours
			// by sending a ResourceRequest
			// RequestResources.Request req = new RequestResources.Request(self,
			// self,
			// event.getNumCpus(), event.getMemoryInMbs());
			// trigger(req, networkPort);

			/*
			 * // Direct allocation trigger(new AllocateResources(event.getId(),
			 * event.getNumCpus(), event.getMemoryInMbs(),
			 * event.getTimeToHoldResource()),
			 * worker.getNegative(WorkerPort.class));
			 */
			Task t = new Task(event.getId(), event.getNumCpus(), event.getMemoryInMbs(),
				event.getTimeToHoldResource()
			);
			probe(t);
		}
	};

	Handler<TManSample> handleTManSample = new Handler<TManSample>() {
		@Override
		public void handle(TManSample event) {
			// TODO:
		}
	};

}
