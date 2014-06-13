package resourcemanager.system.peer.rm.task;

import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

public class TaskDone extends Timeout {

	public final long referenceId;
	
	public TaskDone(ScheduleTimeout request, long referenceId) {
		super(request);
		this.referenceId = referenceId;
	}
}
