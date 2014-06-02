package resourcemanager.system.peer.rm;

import resourcemanager.system.peer.rm.task.Task;
import resourcemanager.system.peer.rm.task.TaskPlaceholder;
import se.sics.kompics.Event;

public class Resources {

	private static class Base extends Event {

		private TaskPlaceholder task;

		public Base(TaskPlaceholder t) {
			this.task = t;
		}

		public TaskPlaceholder getTask() {
			return task;
		}
	}

	public static class Reserve extends Base {

		public Reserve(TaskPlaceholder t) {
			super(t);
		}
	}

	public static class Confirm extends Base {

		public Confirm(TaskPlaceholder t) {
			super(t);
		}
	}

	public static class Allocate extends Event {
		public long referencId;
		public Task task;

		public Allocate(long referenceId, Task t) {
			this.task = t;
			this.referencId = referenceId;

		}

		public Task getTask() {
			return task;
		}
	}

	public static class Cancel extends Event {
		public long referencId;

		public Cancel(long referenceId) {
			this.referencId = referenceId;

		}
	}
}
