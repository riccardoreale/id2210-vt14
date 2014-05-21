package common.peer;

public class Task {

	protected final long id;
	protected final int numCpus;
	protected final int memoryInMbs;
	protected final int timeToHoldResource;

	public Task(long id, int numCpus, int memoryInMbs, int timeToHoldResource) {
		super();
		this.id = id;
		this.numCpus = numCpus;
		this.memoryInMbs = memoryInMbs;
		this.timeToHoldResource = timeToHoldResource;
	}

	public long getId() {
		return id;
	}

	public int getTimeToHoldResource() {
		return timeToHoldResource;
	}

	public int getMemoryInMbs() {
		return memoryInMbs;
	}

	public int getNumCpus() {
		return numCpus;
	}

}