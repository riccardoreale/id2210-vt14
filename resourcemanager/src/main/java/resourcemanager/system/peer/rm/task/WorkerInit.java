package resourcemanager.system.peer.rm.task;

import se.sics.kompics.Init;
import se.sics.kompics.address.Address;

import common.peer.AvailableResources;

public class WorkerInit extends Init {

	private final Address peerSelf;
	private final AvailableResources availableResources;

	public WorkerInit(Address peerSelf, AvailableResources availableResources) {
		super();
		this.peerSelf = peerSelf;
		this.availableResources = availableResources;
	}

	public Address getPeerSelf() {
		return peerSelf;
	}

	public AvailableResources getAvailableResources() {
		return availableResources;
	}

}
