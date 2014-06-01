package resourcemanager.system.peer.rm;

import resourcemanager.system.peer.rm.task.Task;
import se.sics.kompics.Event;
import se.sics.kompics.address.Address;

public class Resources {

	private static class Base extends Event {
		private final long id;
		private final int numCpus;
		private final int memoryInMbs;

		public Base(long id, int numCpus, int memoryInMbs) {
			this.id = id;
			this.numCpus = numCpus;
			this.memoryInMbs = memoryInMbs;
		}

		public long getId() {
			return id;
		}

		public int getMemoryInMbs() {
			return memoryInMbs;
		}

		public int getNumCpus() {
			return numCpus;
		}
	}

	public static class Reserve extends Base {
		public final Address taskMaster;

		public Reserve(Task t, Address taskMaster) {
			super(t.getId(), t.getNumCpus(), t.getMemoryInMbs());
			this.taskMaster = taskMaster;
		}
	}

	public static class Confirm extends Event {
		public Task task;
		public Address master;

		public Confirm(Task t, Address master) {
			this.task = t;
			this.master = master;
		}
	}

	public static class Allocate extends Event {
		public long referencId;
		public Task task;

		public Allocate(long referenceId, Task t) {
			this.referencId = referenceId;
			this.task = t;

		}
	}

	public static class Cancel extends Event {
		public long referencId;

		public Cancel(long referenceId) {
			this.referencId = referenceId;

		}
	}
}
