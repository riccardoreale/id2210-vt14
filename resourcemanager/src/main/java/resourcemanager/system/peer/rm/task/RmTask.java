package resourcemanager.system.peer.rm.task;

import se.sics.kompics.address.Address;

public class RmTask extends Task {

	public RmTask(long id, int numCpus, int memoryInMbs, int timeToHoldResource) {
		super(id, numCpus, memoryInMbs, timeToHoldResource);
	}

	/* Unique identification of virtual task (probed) */
	Address taskMaster;
	long localId;
}