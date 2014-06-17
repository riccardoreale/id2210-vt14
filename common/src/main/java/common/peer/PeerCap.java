package common.peer;

import se.sics.kompics.address.Address;

/**
 * Global capabilities of the peer, in terms of resources, regardless if they
 * are free or busy
 */
public class PeerCap implements Comparable<PeerCap> {

	private final static boolean USE_QUEUE_LEN = true;
	public final Address address;
	public final int maxCpu;
	public final int maxMemory;

	public final int availableCpu;
	public final int availableMemory;

	public final int queueLength;

	public PeerCap(Address address, int maxCpu, int maxMemory,
			int availableCpu, int availableMemory, int queueLength) {
		super();
		this.address = address;
		this.maxCpu = maxCpu;
		this.maxMemory = maxMemory;

		this.availableCpu = availableCpu;
		this.availableMemory = availableMemory;

		this.queueLength = queueLength;
	}

	public boolean canRun(int nCpus, int memoryMb) {
		return nCpus <= this.maxCpu && memoryMb <= this.maxMemory;
	}

	@Override
	public int compareTo(PeerCap that) {
		int v1 = this.maxMemory;
		int v2 = that.maxMemory;
		if (v1 < v2)
			return -1;
		if (v2 > v1)
			return -1;
		v1 = this.maxCpu;
		v2 = that.maxCpu;
		if (v1 < v2)
			return -1;
		if (v2 > v1)
			return -1;
		return 0;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		PeerCap otherP = (PeerCap) other;
		if (otherP.getAddress().equals(getAddress()))
			return true;

		return false;
	}

	@Override
	public int hashCode() {
		return getAddress().hashCode();
	}

	public Address getAddress() {
		return address;
	}

	public int getAvailableCpu() {
		return availableCpu;
	}

	public int getAvailableMemory() {
		return availableMemory;
	}

	public int getQueueLength() {
		return queueLength;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[").append(address.getIp()).append(" - ");
		sb.append(getUtilityFunction()).append("]");

		return sb.toString();
	}

	/**
	 * right now the utility function uses only the available cpu since the load
	 * study is based only on the cpu. We didn't search for a better solution
	 * comprehensive of the memory because anyhow random walks seems to perform
	 * better.
	 * 
	 * Adding the waiting queue lenght in the utility function doesn't improve
	 * results since the queued tasks are just placeholder and may be cancelled.
	 * 
	 * @return
	 */
	public int getUtilityFunction() {
		int u = getAvailableCpu();
		if (USE_QUEUE_LEN && u == 0)
			u -= queueLength;
		return u;
	}
}
