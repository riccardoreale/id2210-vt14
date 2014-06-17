package resourcemanager.system.peer.rm;

import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

public class ProbesTimeout extends Timeout {

	public final long referenceId;
	
	public ProbesTimeout(ScheduleTimeout request, long referenceId) {
		super(request);
		this.referenceId = referenceId;
	}
}
