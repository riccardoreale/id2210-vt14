package fdet.system.evts;

import se.sics.kompics.Init;
import se.sics.kompics.address.Address;

public class DetectorInit extends Init {
	
	public final Address self;
	public final long pingTimeout;
	
	public DetectorInit(Address self, long minTimeout) {
		this.self = self;
		this.pingTimeout = minTimeout;
	}

}
