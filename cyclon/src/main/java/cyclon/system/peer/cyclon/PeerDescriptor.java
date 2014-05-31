package cyclon.system.peer.cyclon;

import java.io.Serializable;

import common.peer.PeerCap;

public class PeerDescriptor implements Comparable<PeerDescriptor>, Serializable {
	private static final long serialVersionUID = 1906679375438244117L;
	private final PeerCap peerCap;
	private int age;

	public PeerDescriptor(PeerCap peerCap) {
		this.peerCap = peerCap;
		this.age = 0;
	}

	public int incrementAndGetAge() {
		age++;
		return age;
	}

	public int getAge() {
		return age;
	}

	public PeerCap getPeerCap() {
		return peerCap;
	}

	@Override
	public int compareTo(PeerDescriptor that) {
		if (this.age > that.age)
			return 1;
		if (this.age < that.age)
			return -1;
		return 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((peerCap == null) ? 0 : peerCap.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PeerDescriptor other = (PeerDescriptor) obj;
		if (peerCap == null) {
			if (other.peerCap != null)
				return false;
		} else if (!peerCap.equals(other.peerCap))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return peerCap + "";
	}

}
