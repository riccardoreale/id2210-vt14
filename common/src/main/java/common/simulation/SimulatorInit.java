package common.simulation;

import common.configuration.CyclonConfiguration;
import common.configuration.DataCenterConfiguration;
import common.configuration.FdetConfiguration;
import common.configuration.RmConfiguration;
import common.configuration.TManConfiguration;
import se.sics.kompics.Init;
import se.sics.kompics.p2p.bootstrap.BootstrapConfiguration;

public final class SimulatorInit extends Init {

    private final BootstrapConfiguration bootstrapConfiguration;
    private final CyclonConfiguration cyclonConfiguration;
    private final TManConfiguration tmanConfiguration;
    private final RmConfiguration aggregationConfiguration;
    private final DataCenterConfiguration dataCenterConfiguration;
    private final FdetConfiguration fdetConfiguration;
	
    public SimulatorInit(BootstrapConfiguration bootstrapConfiguration,
            CyclonConfiguration cyclonConfiguration, TManConfiguration tmanConfiguration,
            RmConfiguration aggregationConfiguration,
            DataCenterConfiguration dataCenterConfiguration,
            FdetConfiguration fdetConfiguration) {
        super();
        this.bootstrapConfiguration = bootstrapConfiguration;
        this.cyclonConfiguration = cyclonConfiguration;
        this.tmanConfiguration = tmanConfiguration;
        this.aggregationConfiguration = aggregationConfiguration;
        this.dataCenterConfiguration = dataCenterConfiguration;
        this.fdetConfiguration = fdetConfiguration;
    }

    public RmConfiguration getAggregationConfiguration() {
        return aggregationConfiguration;
    }
	
    public BootstrapConfiguration getBootstrapConfiguration() {
        return this.bootstrapConfiguration;
    }
	
    public CyclonConfiguration getCyclonConfiguration() {
        return this.cyclonConfiguration;
    }
	
    public TManConfiguration getTmanConfiguration() {
        return this.tmanConfiguration;
    }

	public DataCenterConfiguration getDataCenterConfiguration() {
		return this.dataCenterConfiguration;
	}

	public FdetConfiguration getFdetConfiguration() {
		return this.fdetConfiguration;
	}

}
