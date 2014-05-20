package resourcemanager.system.peer.rm.task;

public class Task {

	private final long id;
	private final int numCpus;
	private final int memoryInMbs;
	private final long enqueueTime; 
	private long allocateTime;
	private long deallocateTime;
	
	/* Payload */
	private final int timeToHoldResource;

	public Task(long id, int numCpus, int memoryInMbs,
			int timeToHoldResource) {
		this.id = id;
		this.numCpus = numCpus;
		this.memoryInMbs = memoryInMbs;
		this.timeToHoldResource = timeToHoldResource;
		this.enqueueTime = System.currentTimeMillis();
	}
	
	public void allocate () {
		this.allocateTime = System.currentTimeMillis();
	}
	
	public void deallocate () {
		this.deallocateTime = System.currentTimeMillis();
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
	
	public long getTotalTime() {
		return deallocateTime - enqueueTime;
	}

	public long getQueueTime() {
		return allocateTime - enqueueTime;
	}
}