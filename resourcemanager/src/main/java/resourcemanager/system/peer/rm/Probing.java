package resourcemanager.system.peer.rm;

import resourcemanager.system.peer.rm.task.Task;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;

public class Probing {

	public static class Request extends Message {
		private static final long serialVersionUID = 6925804371732048383L;
		public final Task task;

		protected Request(Address self, Address target, Task t) {
			super(self, target);
			this.task = t;
		}
	}

	public static class Response extends Message {
		private static final long serialVersionUID = 1091630415746168650L;
		public final Task task;

		protected Response(Address self, Address target, Task t) {
			super(self, target);
			this.task = t;
		}
	}

	public static class Allocate extends Message {
		private static final long serialVersionUID = 2238633324998152336L;
		public final Task task;
		public final long refId;

		protected Allocate(Address self, Address target, long referenceId,
				Task actualTask) {
			super(self, target);
			this.refId = referenceId;
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