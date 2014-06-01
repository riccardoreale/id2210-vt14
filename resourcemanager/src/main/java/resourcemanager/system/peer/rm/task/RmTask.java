package resourcemanager.system.peer.rm.task;

import se.sics.kompics.address.Address;

public class RmTask extends Task {

	/* Unique identification of virtual task (probed) */
	public Address taskMaster;
	long localId;

	private boolean executeDirectly = false;

	public RmTask(long id, int numCpus, int memoryInMbs, int timeToHoldResource) {
		super(id, numCpus, memoryInMbs, timeToHoldResource);
	}

	public Address getTaskMaster() {
		return taskMaster;
	}

	public boolean isExecuteDirectly() {
		return executeDirectly;
	}
}