package system.peer;

import resourcemanager.system.peer.rm.task.AvailableResourcesImpl;
import se.sics.kompics.Init;
import se.sics.kompics.address.Address;
import se.sics.kompics.p2p.bootstrap.BootstrapConfiguration;

import common.configuration.CyclonConfiguration;
import common.configuration.FdetConfiguration;
import common.configuration.RmConfiguration;
import common.configuration.TManConfiguration;

public final class PeerInit extends Init {

	private final Address peerSelf;
	private final BootstrapConfiguration bootstrapConfiguration;
	private final CyclonConfiguration cyclonConfiguration;
	private final RmConfiguration applicationConfiguration;
	private final AvailableResourcesImpl availableResources;
	private final TManConfiguration tmanConfiguration;
	private final FdetConfiguration fdetConfiguration;

	public PeerInit(Address peerSelf,
			BootstrapConfiguration bootstrapConfiguration,
			CyclonConfiguration cyclonConfiguration,
			RmConfiguration applicationConfiguration,
			AvailableResourcesImpl availableResources,
			TManConfiguration tmanConfiguration,
			FdetConfiguration fdetConfiguration) {
		super();
		this.peerSelf = peerSelf;
		this.bootstrapConfiguration = bootstrapConfiguration;
		this.cyclonConfiguration = cyclonConfiguration;
		this.applicationConfiguration = applicationConfiguration;
		this.availableResources = availableResources;
		this.tmanConfiguration = tmanConfiguration;
		this.fdetConfiguration = fdetConfiguration;
	}

	public long getFdTimeout() {
		return fdetConfiguration.timeout;
	}

	public AvailableResourcesImpl getAvailableResources() {
		return availableResources;
	}

	public Address getPeerSelf() {
		return this.peerSelf;
	}

	public BootstrapConfiguration getBootstrapConfiguration() {
		return this.bootstrapConfiguration;
	}

	public CyclonConfiguration getCyclonConfiguration() {
		return this.cyclonConfiguration;
	}

	public RmConfiguration getApplicationConfiguration() {
		return this.applicationConfiguration;
	}

	public TManConfiguration getTmanConfiguration() {
		return tmanConfiguration;
	}
}
