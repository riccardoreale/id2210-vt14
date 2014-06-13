package resourcemanager.system.peer.rm;

import resourcemanager.system.peer.rm.task.Task;
import resourcemanager.system.peer.rm.task.TaskPlaceholder;
import se.sics.kompics.Event;

public class Resources {

	private static class TaskEvent extends Event {

		private TaskPlaceholder task;

		public TaskEvent(TaskPlaceholder t) {
			this.task = t;
		}

		public TaskPlaceholder getTask() {
			return task;
		}
	}

	public static class Reserve extends TaskEvent {

		public Reserve(TaskPlaceholder t) {
			super(t);
		}
	}

	public static class Confirm extends TaskEvent {

		public Confirm(TaskPlaceholder t) {
			super(t);
		}
	}
	
	private static class RefEvent extends Event {
		public final long referenceId;
		
		public RefEvent(long referenceId) {
			this.referenceId = referenceId;
		}
	}

	public static class Allocate extends RefEvent {
		public Task task;

		public Allocate(long referenceId, Task t) {
			super(referenceId);
			this.task = t;
		}

		public Task getTask() {
			return task;
		}
	}

	public static class Cancel extends RefEvent {
		public Cancel(long referenceId) {
			super(referenceId);
		}
	}

	public static class Completed extends RefEvent {
		public Completed(long referenceId) {
			super(referenceId);
		}
	}
}
