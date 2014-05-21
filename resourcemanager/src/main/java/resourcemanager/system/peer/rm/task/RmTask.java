package resourcemanager.system.peer.rm.task;

import common.peer.Task;

public class RmTask extends Task {

	final long enqueueTime; 
	private long allocateTime;
	long deallocateTime;
	
	public RmTask(long id, int numCpus, int memoryInMbs,
			int timeToHoldResource) {
		super(id, numCpus, memoryInMbs, timeToHoldResource);
		this.enqueueTime = System.currentTimeMillis();
	}
	
	public void allocate () {
		this.allocateTime = System.currentTimeMillis();
	}
	
	public void deallocate () {
		this.deallocateTime = System.currentTimeMillis();
	}
	
	public long getQueueTime() {
		return allocateTime - enqueueTime;
	}

	public long getTotalTime() {
		return deallocateTime - enqueueTime;
	}
}