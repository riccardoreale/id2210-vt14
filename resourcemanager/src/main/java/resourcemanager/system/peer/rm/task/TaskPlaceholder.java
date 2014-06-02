package resourcemanager.system.peer.rm.task;

import se.sics.kompics.address.Address;

public class TaskPlaceholder extends Task {

	/* Unique identification of virtual task (probed) */
	public Address taskMaster;
	public boolean executeDirectly = false;

	public TaskPlaceholder(long id, int numCpus, int memoryInMbs,
			int timeToHoldResource, Address taskMaster) {
		super(id, numCpus, memoryInMbs, timeToHoldResource);
		this.taskMaster = taskMaster;

	}

	public Address getTaskMaster() {
		return taskMaster;
	}

	public boolean isExecuteDirectly() {
		return executeDirectly;
	}
}