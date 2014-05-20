package resourcemanager.system.peer.rm.task;

import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

public class TaskDone extends Timeout {

	public TaskDone(SchedulePeriodicTimeout request) {
		super(request);
	}


	public TaskDone(ScheduleTimeout request) {
		super(request);
	}
}
