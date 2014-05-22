package resourcemanager.system.peer.rm;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import common.peer.PeerCap;
import common.peer.PeerDescriptor;
import common.utils.FuncTools;
import common.utils.FuncTools.Proposition;

import resourcemanager.system.peer.rm.task.Task;
import se.sics.kompics.ComponentDefinition;
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
	
	private final Positive<Network> net;
	private final Address self;
	private final Map<Long, Task> waiting = new HashMap<Long, Task>();
	private final Set<Address> outstanding = new HashSet<Address>();
	private final Collection<PeerCap> neighbors;
	
	public Prober (Address self, Positive<Network> net, Collection<PeerCap> neighbors) {
		this.self = self;
		this.net = net;
		this.neighbors = neighbors;
	}
	
	public void subscribe (final Task t) {
		waiting.put(t.getId(), t);
		
		Proposition<PeerCap> p = new Proposition<PeerCap>() {
			@Override
			public boolean eval (PeerCap param) {
				return param.memoryMb >= t.getMemoryInMbs()
					&& param.nCpus >= t.getNumCpus()
					&& ! outstanding.contains(param.address);
			}
		};

		int put = PROBES_PER_JOB;	// × t.njobs
		List<PeerCap> sel = FuncTools.filter(neighbors, p);
		Collections.shuffle(sel);
		for (PeerCap cap : sel) {
			if (put --> 0) break;

			outstanding.add(cap.address);
			trigger(new Request(self, cap.address), net);
		}
	}
}