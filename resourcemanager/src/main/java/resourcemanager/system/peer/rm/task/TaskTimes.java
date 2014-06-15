package resourcemanager.system.peer.rm.task;

public class TaskTimes {
	protected long enqueueTime = -1;
	protected long allocateTime = -1;
	protected long deallocateTime = -1;
	
	public void queue() {
		this.enqueueTime = System.currentTimeMillis();
	}

	public void allocate() {
		this.allocateTime = System.currentTimeMillis();
	}

	public void deallocate() {
		this.deallocateTime = System.currentTimeMillis();
	}

	public long getQueueTime() {
		return allocateTime - enqueueTime;
	}

	public long getTotalTime() {
		return deallocateTime - enqueueTime;
	}

}
