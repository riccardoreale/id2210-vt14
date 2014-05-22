package common.simulation;

import se.sics.kompics.Event;

public final class ClientRequestResource extends Event {

	private final long id;
	private final int numCpus;
	private final int memoryInMbs;
	private final int timeToHoldResource;
	private final int batchSize;

	public ClientRequestResource(long id, int batchSize, int numCpus,
			int memoryInMbs, int timeToHoldResource) {
		this.id = id;
		this.batchSize = batchSize;
		this.numCpus = numCpus;
		this.memoryInMbs = memoryInMbs;
		this.timeToHoldResource = timeToHoldResource;
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

	public int getBatchSize() {
		return batchSize;
	}

}
