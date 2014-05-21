package resourcemanager.system.peer.rm.task;

import common.peer.Task;

public class RmTask extends Task {

	public RmTask(long id, int numCpus, int memoryInMbs, int timeToHoldResource) {
		super(id, numCpus, memoryInMbs, timeToHoldResource);
	}

}