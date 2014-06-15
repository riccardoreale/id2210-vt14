package resourcemanager.system.peer.rm.task;

import common.simulation.TaskResources;

public class Task extends TaskTimes {

	public final long id;
	public final TaskResources required;
	public final long timeToHoldResource;

	public Task(long id, TaskResources required, long timeToHoldResource) {
		this.id = id;
		this.required = required;
		this.timeToHoldResource = timeToHoldResource;
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

	public Task copy() {
		return new Task(this.id, this.required, this.timeToHoldResource);
	}

}