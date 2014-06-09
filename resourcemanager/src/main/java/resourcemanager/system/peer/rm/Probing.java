package resourcemanager.system.peer.rm;

import resourcemanager.system.peer.rm.task.Task;
import resourcemanager.system.peer.rm.task.TaskPlaceholder;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;

public class Probing {

	public static class Request extends Message {
		private static final long serialVersionUID = 6925804371732048383L;
		public final TaskPlaceholder task;
		public final Address taskMaster;
		public final int count;

		protected Request(Address self, Address target, TaskPlaceholder t,
				Address taskMaster, int count) {
			super(self, target);
			this.task = t;
			this.taskMaster = taskMaster;
			this.count = count;
		}
	}

	public static class GotRequest extends Message {
		private static final long serialVersionUID = 1091630415746168650L;
		public final TaskPlaceholder task;

		protected GotRequest(Address self, Address target, TaskPlaceholder t) {
			super(self, target);
			this.task = t;
		}
	}

	public static class Response extends Message {
		private static final long serialVersionUID = 1091630415746168650L;
		public final long id;

		protected Response(Address self, Address target, long id) {
			super(self, target);
			this.id = id;
		}
	}

	public static class Allocate extends Message {
		private static final long serialVersionUID = 2238633324998152336L;
		public final Task task;
		public final long referenceId;

		protected Allocate(Address self, Address target, long referenceId,
				Task actualTask) {
			super(self, target);
			this.referenceId = referenceId;
			this.task = actualTask;
		}
	}

	public static class Cancel extends Message {
		private static final long serialVersionUID = 3997942241176863428L;
		public long refId;

		protected Cancel(Address self, Address target, long referenceId) {
			super(self, target);
			this.refId = referenceId;
		}
	}
}