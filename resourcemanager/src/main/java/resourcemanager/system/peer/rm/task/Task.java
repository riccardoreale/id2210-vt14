package resourcemanager.system.peer.rm.task;

public class Task extends TaskResources {

	protected long enqueueTime;
	protected long allocateTime;
	protected long deallocateTime;

	public Task(long id, int numCpus, int memoryInMbs, int timeToHoldResource) {
		super(id, numCpus, memoryInMbs, timeToHoldResource);
		this.enqueueTime = -1;
		this.allocateTime = -1;
		this.deallocateTime = -1;
	}

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

	@Override
	public int hashCode() {
		return (int) (id % Integer.MAX_VALUE);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Task other = (Task) obj;
		return id == other.id;
	}

}