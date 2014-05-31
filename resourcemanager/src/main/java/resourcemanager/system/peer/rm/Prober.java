package resourcemanager.system.peer.rm;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import common.peer.PeerCap;
import common.utils.FuncTools;
import common.utils.FuncTools.Proposition;

import resourcemanager.system.peer.rm.task.Task;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Event;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.network.Network;

public class Prober extends ComponentDefinition {

	private static final int PROBES_PER_JOB = 2;

	public static class Request extends Message {
		private static final long serialVersionUID = 6925804371732048383L;

		protected Request(Address self, Address target) {
			super(self, target);
		}
	}

	public static class Response extends Message {
		private static final long serialVersionUID = 1091630415746168650L;

		protected Response(Request ansTo) {
			super(ansTo.getDestination(), ansTo.getSource());
		}
	}

	public static class Cancel extends Message {
		private static final long serialVersionUID = 3997942241176863428L;

		protected Cancel(Address self, Address target) {
			super(self, target);
		}
	}

	public static class NodeReady extends Event {
		public final PeerCap nodeCap;

		public NodeReady(PeerCap nodeCap) {
			this.nodeCap = nodeCap;
		}
	}

	private final Positive<Network> net;
	private final Address self;

	private final Collection<PeerCap> neighbors;
	private final Queue<Task> waiting = new LinkedList<Task>();
	private final Map<Address, PeerCap> outstanding = new HashMap<Address, PeerCap>();

	public Prober(Address self, Positive<Network> net,
			Collection<PeerCap> neighbors) {
		this.self = self;
		this.net = net;
		this.neighbors = neighbors;
	}

	public void subscribe(final Task t) {
		assert !waiting.contains(t);
		waiting.add(t);

		Proposition<PeerCap> p = new Proposition<PeerCap>() {
			@Override
			public boolean eval(PeerCap param) {
				return param.memoryMb >= t.getMemoryInMbs()
						&& param.nCpus >= t.getNumCpus()
						&& !outstanding.containsKey(param.address);
			}
		};

		/* Select only among nodes which can satisfy this task */
		List<PeerCap> sel = FuncTools.filter(neighbors, p);
		Collections.shuffle(sel);

		int put = PROBES_PER_JOB; // × t.njobs
		for (PeerCap cap : sel) {
			if (put-- > 0)
				break;

			outstanding.put(cap.address, cap);
			trigger(new Request(self, cap.address), net);
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
			trigger(new Cancel(self, peer), net);
			for (Address to : outstanding.keySet()) {
				trigger(new Cancel(self, to), net);
			}
		}
	}

	public final Handler<Response> handleResponse = new Handler<Prober.Response>() {
		@Override
		public void handle(Response resp) {
			Address peer = resp.getSource();
			PeerCap cap = outstanding.remove(peer);
			if (cap != null) {
				serve(peer, cap);
			}
			assert false : "Got orphan of a dead node";
		}
	};
}