package common.peer;

import se.sics.kompics.address.Address;

/**
 * Global capabilities of the peer, in terms of resources, regardless if they
 * are free or busy
 */
public class PeerCap implements Comparable<PeerCap> {
	public final Address address;
	public final int maxCpu;
	public final int maxMemory;

	public PeerCap(Address address, int maxCpu, int maxMemory) {
		super();
		this.address = address;
		this.maxCpu = maxCpu;
		this.maxMemory = maxMemory;
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

	public Address getAddress() {
		return address;
	}
}
