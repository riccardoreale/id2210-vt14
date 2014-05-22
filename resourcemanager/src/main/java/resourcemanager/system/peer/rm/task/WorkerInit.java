package resourcemanager.system.peer.rm.task;

import se.sics.kompics.Init;
import se.sics.kompics.address.Address;

public class WorkerInit extends Init {

	private final Address peerSelf;
	private final AvailableResourcesImpl availableResources;

	public WorkerInit(Address peerSelf,
			AvailableResourcesImpl availableResources) {
		super();
		this.peerSelf = peerSelf;
		this.availableResources = availableResources;
	}

	public Address getPeerSelf() {
		return peerSelf;
	}

	public AvailableResourcesImpl getAvailableResources() {
		return availableResources;
	}

}
