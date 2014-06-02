package resourcemanager.system.peer.rm.task;

public class TaskResources {

	protected final long id;
	protected final int numCpus;
	protected final int memoryInMbs;
	protected final int timeToHoldResource;

	public TaskResources(long id, int numCpus, int memoryInMbs,
			int timeToHoldResource) {
		this.id = id;
		this.numCpus = numCpus;
		this.memoryInMbs = memoryInMbs;
		this.timeToHoldResource = timeToHoldResource;
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

	public int getTimeToHoldResource() {
		return timeToHoldResource;
	}

}