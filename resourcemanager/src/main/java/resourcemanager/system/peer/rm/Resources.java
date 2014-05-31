package resourcemanager.system.peer.rm;

import se.sics.kompics.Event;

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
		public Reserve(long id, int numCpus, int memoryInMbs) {
			super(id, numCpus, memoryInMbs);
		}
	}

	public static class Allocate extends Base {
		private final int timeToHoldResource;

		public Allocate(long id, int numCpus, int memoryInMbs,
						int timeToHoldResource) {
			super(id, numCpus, memoryInMbs);
			this.timeToHoldResource = timeToHoldResource;
		}

		public int getTimeToHoldResource() {
			return timeToHoldResource;
		}

	}
}
