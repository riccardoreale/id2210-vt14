package resourcemanager.system.peer.rm;

import resourcemanager.system.peer.rm.task.AvailableResourcesImpl;
import se.sics.kompics.Init;
import se.sics.kompics.address.Address;

import common.configuration.RmConfiguration;

public final class RmInit extends Init {

	private final Address peerSelf;
	private final RmConfiguration configuration;
	private final AvailableResourcesImpl availableResources;

	public RmInit(Address peerSelf, RmConfiguration configuration,
			AvailableResourcesImpl availableResources) {
		super();
		this.peerSelf = peerSelf;
		this.configuration = configuration;
		this.availableResources = availableResources;

	}

	public AvailableResourcesImpl getAvailableResources() {
		return availableResources;
	}

	public Address getSelf() {
		return this.peerSelf;
	}

	public RmConfiguration getConfiguration() {
		return this.configuration;
	}
}
