package fdet.system.evts;

import se.sics.kompics.address.Address;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

public class DetectorTimeout extends Timeout {
	public final Address ref;
	
	public DetectorTimeout(ScheduleTimeout req, Address ref) {
		super(req);
		this.ref = ref;
	}
}
