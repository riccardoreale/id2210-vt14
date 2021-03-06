package system.peer;

import java.util.LinkedList;
import java.util.Set;

import resourcemanager.system.peer.rm.ResourceManager;
import resourcemanager.system.peer.rm.RmInit;
import resourcemanager.system.peer.rm.task.AvailableResourcesImpl;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.p2p.bootstrap.BootstrapCompleted;
import se.sics.kompics.p2p.bootstrap.BootstrapRequest;
import se.sics.kompics.p2p.bootstrap.BootstrapResponse;
import se.sics.kompics.p2p.bootstrap.P2pBootstrap;
import se.sics.kompics.p2p.bootstrap.PeerEntry;
import se.sics.kompics.p2p.bootstrap.client.BootstrapClient;
import se.sics.kompics.p2p.bootstrap.client.BootstrapClientInit;
import se.sics.kompics.timer.Timer;
import tman.system.peer.tman.TMan;
import tman.system.peer.tman.TManInit;
import tman.system.peer.tman.TManSamplePort;

import common.configuration.CyclonConfiguration;
import common.configuration.RmConfiguration;
import common.configuration.TManConfiguration;
import common.peer.PeerDescriptor;

import cyclon.system.peer.cyclon.Cyclon;
import cyclon.system.peer.cyclon.CyclonInit;
import cyclon.system.peer.cyclon.CyclonJoin;
import cyclon.system.peer.cyclon.CyclonPort;
import cyclon.system.peer.cyclon.CyclonSamplePort;
import cyclon.system.peer.cyclon.JoinCompleted;
import fdet.system.Detector;
import fdet.system.evts.DetectorInit;
import fdet.system.evts.FdetPort;

public final class Peer extends ComponentDefinition {

	Positive<RmPort> rmPort = positive(RmPort.class);

	Positive<Network> network = positive(Network.class);
	Positive<Timer> timer = positive(Timer.class);

	private Component cyclon, tman, rm, bootstrap, fdet;
	private Address self;
	private int bootstrapRequestPeerCount;
	private boolean bootstrapped;
	private RmConfiguration rmConfiguration;

	private AvailableResourcesImpl availableResources;

	public Peer() {
		cyclon = create(Cyclon.class);
		tman = create(TMan.class);
		rm = create(ResourceManager.class);
		bootstrap = create(BootstrapClient.class);
		fdet = create(Detector.class);

		connect(network, rm.getNegative(Network.class));
		connect(network, cyclon.getNegative(Network.class));
		connect(network, bootstrap.getNegative(Network.class));
		connect(network, tman.getNegative(Network.class));
		connect(network, fdet.getNegative(Network.class));

		connect(timer, rm.getNegative(Timer.class));
		connect(timer, cyclon.getNegative(Timer.class));
		connect(timer, bootstrap.getNegative(Timer.class));
		connect(timer, tman.getNegative(Timer.class));
		connect(timer, fdet.getNegative(Timer.class));

		connect(cyclon.getPositive(CyclonSamplePort.class),
				rm.getNegative(CyclonSamplePort.class));
		connect(cyclon.getPositive(CyclonSamplePort.class),
				tman.getNegative(CyclonSamplePort.class));
		connect(tman.getPositive(TManSamplePort.class),
				rm.getNegative(TManSamplePort.class));

		connect(rmPort, rm.getNegative(RmPort.class));

		subscribe(handleInit, control);
		subscribe(handleJoinCompleted, cyclon.getPositive(CyclonPort.class));
		subscribe(handleBootstrapResponse,
				bootstrap.getPositive(P2pBootstrap.class));
		
		connect(fdet.getPositive(FdetPort.class), rm.getNegative(FdetPort.class));
	}

	Handler<PeerInit> handleInit = new Handler<PeerInit>() {
		@Override
		public void handle(PeerInit init) {
			self = init.getPeerSelf();
			CyclonConfiguration cyclonConfiguration = init
					.getCyclonConfiguration();
			TManConfiguration tmanconfig = init.getTmanConfiguration();
			rmConfiguration = init.getApplicationConfiguration();
			bootstrapRequestPeerCount = cyclonConfiguration
					.getBootstrapRequestPeerCount();

			availableResources = init.getAvailableResources();

			trigger(new CyclonInit(cyclonConfiguration, availableResources),
					cyclon.getControl());

			trigger(new TManInit(self, tmanconfig, availableResources),
					tman.getControl());
			trigger(new BootstrapClientInit(self,
					init.getBootstrapConfiguration()), bootstrap.getControl());
			BootstrapRequest request = new BootstrapRequest("Cyclon",
					bootstrapRequestPeerCount);
			trigger(request, bootstrap.getPositive(P2pBootstrap.class));
			trigger(new DetectorInit(self, init.getFdTimeout()),
				fdet.getControl()
			);
		}
	};

	Handler<BootstrapResponse> handleBootstrapResponse = new Handler<BootstrapResponse>() {
		@Override
		public void handle(BootstrapResponse event) {
			if (!bootstrapped) {
				Set<PeerEntry> somePeers = event.getPeers();
				LinkedList<Address> cyclonInsiders = new LinkedList<Address>();

				for (PeerEntry peerEntry : somePeers) {
					cyclonInsiders.add(peerEntry.getOverlayAddress()
							.getPeerAddress());
				}
				
				trigger(new CyclonJoin(self, cyclonInsiders),
						cyclon.getPositive(CyclonPort.class));
				bootstrapped = true;
			}
		}
	};

	Handler<JoinCompleted> handleJoinCompleted = new Handler<JoinCompleted>() {
		@Override
		public void handle(JoinCompleted event) {
			trigger(new BootstrapCompleted("Cyclon", new PeerDescriptor(self,
					availableResources.getNumFreeCpus(),
					availableResources.getFreeMemInMbs())),
					bootstrap.getPositive(P2pBootstrap.class));
			trigger(new RmInit(self, rmConfiguration, availableResources),
					rm.getControl());
		}
	};

}
