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
		public final Address taskMaster;
		public final int count;

		protected Request(Address self, Address target, TaskResources required,
				long taskId, Address taskMaster, int count) {
			super(self, target);
			this.required = required;
			this.taskId = taskId;
			this.taskMaster = taskMaster;
			this.count = count;
		}
	}
	
	public static class GotRequest extends RefMessage {
		private static final long serialVersionUID = 1091630415746168650L;

		protected GotRequest(Address self, Address target, long referenceId) {
			super(self, target, referenceId);
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
	
	public static class Completed extends RefMessage {
		private static final long serialVersionUID = -5644837612278254502L;
		protected Completed(Address source, Address destination, long referenceId) {
			super(source, destination, referenceId);
		}
	}
}