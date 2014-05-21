package simulator.core;

import java.net.InetAddress;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import se.sics.ipasdistances.AsIpGenerator;
import se.sics.kompics.ChannelFilter;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.network.Network;
import se.sics.kompics.p2p.bootstrap.BootstrapConfiguration;
import se.sics.kompics.p2p.experiment.dsl.events.TerminateExperiment;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timer;
import simulator.snapshot.Snapshot;
import system.peer.Peer;
import system.peer.PeerInit;
import system.peer.RmPort;

import common.configuration.Configuration;
import common.configuration.CyclonConfiguration;
import common.configuration.DataCenterConfiguration;
import common.configuration.RmConfiguration;
import common.configuration.TManConfiguration;
import common.peer.AvailableResources;
import common.simulation.ClientRequestResource;
import common.simulation.ConsistentHashtable;
import common.simulation.GenerateReport;
import common.simulation.PeerFail;
import common.simulation.PeerJoin;
import common.simulation.SimulatorInit;
import common.simulation.SimulatorPort;

public final class DataCenterSimulator extends ComponentDefinition {
	
	private class Pair<X, Y> {
		public final X fst;
		public final Y snd;
		
		public Pair (X f, Y s) {
			fst = f;
			snd = s;
		}
	}

	Positive<SimulatorPort> simulator = positive(SimulatorPort.class);
	Positive<Network> network = positive(Network.class);
	Positive<Timer> timer = positive(Timer.class);
	private final HashMap<Long, Component> peers;
	private final HashMap<Long, Address> peersAddress;
	private BootstrapConfiguration bootstrapConfiguration;
	private DataCenterConfiguration dataCenterConfiguration;
	private CyclonConfiguration cyclonConfiguration;
	private RmConfiguration rmConfiguration;
	private TManConfiguration tmanConfiguration;
	private Long identifierSpaceSize;
	private ConsistentHashtable<Long> ringNodes;
	private AsIpGenerator ipGenerator = AsIpGenerator.getInstance(125);
	private Map<Address, Pair<Component, AvailableResources>> omniscentOracle = new HashMap<Address, Pair<Component, AvailableResources>>();

	Random r = new Random(System.currentTimeMillis());

	public DataCenterSimulator() {
		peers = new HashMap<Long, Component>();
		peersAddress = new HashMap<Long, Address>();
		ringNodes = new ConsistentHashtable<Long>();

		subscribe(handleInit, control);
		subscribe(handleGenerateReport, timer);
		subscribe(handlePeerJoin, simulator);
		subscribe(handlePeerFail, simulator);
		subscribe(handleTerminateExperiment, simulator);
		subscribe(handleRequestResource, simulator);
	}

	Handler<SimulatorInit> handleInit = new Handler<SimulatorInit>() {
		@Override
		public void handle(SimulatorInit init) {
			peers.clear();

			bootstrapConfiguration = init.getBootstrapConfiguration();
			cyclonConfiguration = init.getCyclonConfiguration();
			rmConfiguration = init.getAggregationConfiguration();
			tmanConfiguration = init.getTmanConfiguration();
			dataCenterConfiguration = init.getDataCenterConfiguration();
			identifierSpaceSize = cyclonConfiguration.getIdentifierSpaceSize();

			// generate periodic report
			int snapshotPeriod = Configuration.SNAPSHOT_PERIOD;
			SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(
					snapshotPeriod, snapshotPeriod);
			spt.setTimeoutEvent(new GenerateReport(spt));
			trigger(spt, timer);

		}
	};

	Handler<ClientRequestResource> handleRequestResource = new Handler<ClientRequestResource>() {
		@Override
		public void handle(ClientRequestResource event) {
			Component peer = null;
			if (dataCenterConfiguration.omniscent) {
				peer = omniscentSelector();
			} else {
				Long successor = ringNodes.getNode(event.getId());
				peer = peers.get(successor);
			}
			trigger(event, peer.getNegative(RmPort.class));
		}

	};

	private Comparator<Pair<Component, AvailableResources>> cmpPeer = new Comparator<Pair<Component, AvailableResources>>() {
		@Override
		public int compare(Pair<Component, AvailableResources> o1,
						   Pair<Component, AvailableResources> o2) {
			int c;

			c = o1.snd.getQueueLength() - o2.snd.getQueueLength();
			if (c != 0)
				return c;

			/* Arbitrary selection of memory as first comparison index */
			c = o1.snd.getFreeMemInMbs()
					- o2.snd.getFreeMemInMbs();
			if (c != 0)
				return c;
			return o1.snd.getNumFreeCpus()
					- o2.snd.getNumFreeCpus();
		}

	};

	private Component omniscentSelector() {
		return Collections.max(omniscentOracle.values(), cmpPeer).fst;
	}

	Handler<PeerJoin> handlePeerJoin = new Handler<PeerJoin>() {
		@Override
		public void handle(PeerJoin event) {
			Long id = event.getPeerId();

			// join with the next id if this id is taken
			Long successor = ringNodes.getNode(id);

			while (successor != null && successor.equals(id)) {
				id = (id + 1) % identifierSpaceSize;
				successor = ringNodes.getNode(id);
			}

			createAndStartNewPeer(id, event.getNumFreeCpus(),
					event.getFreeMemoryInMbs());
			ringNodes.addNode(id);
		}
	};

	Handler<PeerFail> handlePeerFail = new Handler<PeerFail>() {
		@Override
		public void handle(PeerFail event) {
			Long id = ringNodes.getNode(event.getId());

			if (ringNodes.size() == 0) {
				System.err.println("Empty network");
				return;
			}

			ringNodes.removeNode(id);
			stopAndDestroyPeer(id);
		}
	};

	Handler<TerminateExperiment> handleTerminateExperiment = new Handler<TerminateExperiment>() {
		@Override
		public void handle(TerminateExperiment event) {
			System.err.println("Finishing experiment - terminating....");
			System.exit(0);
		}
	};

	Handler<GenerateReport> handleGenerateReport = new Handler<GenerateReport>() {
		@Override
		public void handle(GenerateReport event) {
			Snapshot.report();
		}
	};

	private void createAndStartNewPeer(long id, int numCpus, int memInMb) {
		Component peer = create(Peer.class);
		InetAddress ip = ipGenerator.generateIP();
		Address address = new Address(ip, 8058, (int) id);

		connect(network, peer.getNegative(Network.class),
				new MessageDestinationFilter(address));
		connect(timer, peer.getNegative(Timer.class));

		AvailableResources ar = new AvailableResources(numCpus, memInMb);
		trigger(new PeerInit(address, bootstrapConfiguration,
				cyclonConfiguration, rmConfiguration, ar), peer.getControl());

		trigger(new Start(), peer.getControl());
		peers.put(id, peer);
		peersAddress.put(id, address);
		Snapshot.addPeer(address, ar);
		omniscentOracle.put(address, new Pair<Component, AvailableResources>(peer, ar));
	}

	private void stopAndDestroyPeer(Long id) {
		Component peer = peers.get(id);

		trigger(new Stop(), peer.getControl());

		disconnect(network, peer.getNegative(Network.class));
		disconnect(timer, peer.getNegative(Timer.class));

		peers.remove(id);
		Address addr = peersAddress.remove(id);
		Snapshot.removePeer(addr);

		destroy(peer);
	}

	private final static class MessageDestinationFilter extends
			ChannelFilter<Message, Address> {

		public MessageDestinationFilter(Address address) {
			super(Message.class, address, true);
		}

		@Override
		public Address getValue(Message event) {
			return event.getDestination();
		}
	}
}
