package resourcemanager.system.peer.rm;

import common.simulation.TaskResources;

import resourcemanager.system.peer.rm.task.Task;
import resourcemanager.system.peer.rm.task.TaskPlaceholder;
import se.sics.kompics.Event;
import se.sics.kompics.address.Address;

public class Resources {

	public static class Reserve extends Event {
		public final long taskId;
		public final Address taskMaster;
		public final TaskResources required;

		public Reserve(Address taskMaster, long taskId, TaskResources required) {
			this.taskMaster = taskMaster;
			this.required = required;
			this.taskId = taskId;
		}
	}

	public static class Confirm extends Event {
		public final TaskPlaceholder.Deferred tph;

		public Confirm(TaskPlaceholder.Deferred tph) {
			this.tph = tph;
		}
	}

	public static class Allocate extends Event {
		public final Task task;
		public final Address taskMaster;

		public Allocate(Address taskMaster, Task t) {
			this.task = t;
			this.taskMaster = taskMaster;
		}
	}

	public static class Cancel extends Event {
		public final long taskId;
		public Cancel(long taskId) {
			this.taskId = taskId;
		}
	}

	public static class Completed extends Event {
		public final Task task;
		public final Address taskMaster;

		public Completed(Address taskMaster, Task task) {
			this.task = task;
			this.taskMaster = taskMaster;
		}
	}

}
