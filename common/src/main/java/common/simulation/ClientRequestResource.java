package common.simulation;

import se.sics.kompics.Event;

public final class ClientRequestResource extends Event {

	public final long taskId;
	public final TaskResources required;
	public final int timeToHoldResource;
	public final int batchSize;

	public ClientRequestResource(long id, int batchSize, int numCpus,
			int memoryInMbs, int timeToHoldResource) {
		this.taskId = id;
		this.batchSize = batchSize;
		this.required = new TaskResources(numCpus, memoryInMbs);
		this.timeToHoldResource = timeToHoldResource;
	}
}