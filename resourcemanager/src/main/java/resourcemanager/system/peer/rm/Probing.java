package resourcemanager.system.peer.rm;

import common.simulation.TaskResources;

import resourcemanager.system.peer.rm.task.Task;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;

public class Probing {

	public static class Request extends Message {
		private static final long serialVersionUID = 6925804371732048383L;
		public final TaskResources required;
		public final long taskId;

		protected Request(Address self, Address target, TaskResources required,
				long taskId) {
			super(self, target);
			this.required = required;
			this.taskId = taskId;
		}
	}
	
	private static class RefMessage extends Message {
		private static final long serialVersionUID = -669442991122622071L;
		public final long referenceId;

		protected RefMessage(Address source, Address destination, long referenceId) {
			super(source, destination);
			this.referenceId = referenceId;
		}	
	}

	public static class Response extends RefMessage {
		private static final long serialVersionUID = 1091630415746168650L;

		protected Response(Address self, Address target, long referenceId) {
			super(self, target, referenceId);
		}
	}

	public static class Allocate extends RefMessage {
		private static final long serialVersionUID = 2238633324998152336L;
		public final Task task;

		protected Allocate(Address self, Address target, long referenceId,
		                   Task actualTask) {
			super(self, target, referenceId);
			this.task = actualTask;
		}
	}

	public static class Cancel extends RefMessage {
		private static final long serialVersionUID = 3997942241176863428L;

		protected Cancel(Address self, Address target, long referenceId) {
			super(self, target, referenceId);
		}
	}
}