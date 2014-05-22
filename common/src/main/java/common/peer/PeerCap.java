package common.peer;

import se.sics.kompics.address.Address;

/**
 * Global capabilities of the peer, in terms of resources, regardless if they
 * are free or busy
 */
public class PeerCap {
	public final Address address;
	public final int nCpus;
	public final int memoryMb;

	public PeerCap(Address address, int nCpus, int memoryMb) {
		super();
		this.address = address;
		this.nCpus = nCpus;
		this.memoryMb = memoryMb;
	}

}
