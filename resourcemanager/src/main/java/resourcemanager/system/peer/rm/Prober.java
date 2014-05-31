package resourcemanager.system.peer.rm;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	private final Set<Task> waiting = new HashSet<Task>();
	private final Map<Address, PeerCap> outstanding = new HashMap<Address, PeerCap>();
	private final Map<Address, PeerCap> returned = new HashMap<Address, PeerCap>();

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

	private static Comparator<Task> cmp = new Comparator<Task>() {
		@Override
		public int compare(Task o1, Task o2) {
			int v1 = o1.getMemoryInMbs();
			int v2 = o2.getMemoryInMbs();
			if (v1 < v2)
				return -1;
			if (v2 > v1)
				return -1;
			v1 = o1.getNumCpus();
			v2 = o2.getNumCpus();
			if (v1 < v2)
				return -1;
			if (v2 > v1)
				return -1;
			return 0;
		}
	};

	private void serve() {
		LinkedList<Task> l = new LinkedList<Task>(waiting);
		waiting.clear();
		Collections.sort(l, cmp);

		/* Useless java documentation... Assume this is descending. */
		assert l.size() < 2 || cmp.compare(l.get(0), l.get(1)) <= 0;

		while (l.size() > 0) {
			Task biggest = l.pollLast();
			PeerCap poorest = null;

			for (PeerCap c : returned.values()) {
				if (!c.canRun(biggest.getNumCpus(), biggest.getMemoryInMbs())) {
					continue;
				}

				if (poorest == null || poorest.compareTo(c) > 1) {
					poorest = c;
				}
			}

			if (poorest == null) {
				/*
				 * Only probes for smaller jobs arrived so far or our probes
				 * were all stoled by other tasks.
				 */
				if (outstanding.size() > 0) {
					waiting.add(biggest);
				} else {
					assert false : "Not until we add churn and enable FD.";
					subscribe(biggest);
				}
			}
		}

		/* No waiting tasks left? Send pro-active cancellation */
		if (waiting.isEmpty()) {
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
			if (cap == null)
				return; // Ignore orphan messages.
			returned.put(peer, cap);
			serve();
		}
	};
}