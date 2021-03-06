package common.simulation.scenarios;

import se.sics.kompics.p2p.experiment.dsl.adaptor.Operation;
import se.sics.kompics.p2p.experiment.dsl.adaptor.Operation1;
import se.sics.kompics.p2p.experiment.dsl.adaptor.Operation3;
import se.sics.kompics.p2p.experiment.dsl.adaptor.Operation4;
import se.sics.kompics.p2p.experiment.dsl.events.TerminateExperiment;

import common.simulation.ClientRequestResource;
import common.simulation.Evaluate;
import common.simulation.PeerFail;
import common.simulation.PeerJoin;

@SuppressWarnings("serial")
public class Operations {

	public static Operation3<PeerJoin, Long, Long, Long> peerJoin() {
		return new Operation3<PeerJoin, Long, Long, Long>() {
			@Override
			public PeerJoin generate(Long id, Long numCpus, Long memInMbs) {
				return new PeerJoin(id, numCpus.intValue(), memInMbs.intValue() * 1000);
			}
		};
	}

	public static Operation<Evaluate> evaluate = new Operation<Evaluate>() {
		@Override
		public Evaluate generate() {
			return new Evaluate();
		}
	};

	public static Operation1<PeerFail, Long> peerFail = new Operation1<PeerFail, Long>() {
		@Override
		public PeerFail generate(Long id) {
			return new PeerFail(id);
		}
	};

	public static Operation<TerminateExperiment> terminate = new Operation<TerminateExperiment>() {
		@Override
		public TerminateExperiment generate() {
			return new TerminateExperiment();
		}
	};

	public static Operation4<ClientRequestResource, Long, Long, Long, Long> requestResources() {
		return new Operation4<ClientRequestResource, Long, Long, Long, Long>() {
			@Override
			public ClientRequestResource generate(Long id, Long numCpus,
					Long memInMbs, Long timeToHoldResourceInMilliSecs) {
				return new ClientRequestResource(id, 1, numCpus.intValue(),
						memInMbs.intValue() * 1000,
						timeToHoldResourceInMilliSecs.intValue());
			}
		};
	}
}
