package resourcemanager.system.peer.rm.task;

import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

public class TaskDone extends Timeout {

	public final long id;
	
	public TaskDone(ScheduleTimeout request, long id) {
		super(request);
		this.id = id;
	}
}
