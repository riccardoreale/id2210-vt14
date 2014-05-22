package common.peer;

import se.sics.kompics.address.Address;

/**
 * Global capabilities of the peer, in terms of resources, regardless if they
 * are free or busy
 */
public class PeerCap implements Comparable<PeerCap> {
	public final Address address;
	public final int nCpus;
	public final int memoryMb;

	public PeerCap(Address address, int nCpus, int memoryMb) {
		super();
		this.address = address;
		this.nCpus = nCpus;
		this.memoryMb = memoryMb;
	}
	
	public boolean canRun (int nCpus, int memoryMb) {
		return nCpus <= this.nCpus
			&& memoryMb <= this.memoryMb;
	}

	@Override
	public int compareTo(PeerCap that) {
		int v1 = this.memoryMb;
		int v2 = that.memoryMb;
		if (v1 < v2) return -1;
		if (v2 > v1) return -1;
		v1 = this.nCpus;
		v2 = that.nCpus;
		if (v1 < v2) return -1;
		if (v2 > v1) return -1;
		return 0;
	}

}
