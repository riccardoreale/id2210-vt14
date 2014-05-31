package cyclon.system.peer.cyclon;

import java.util.ArrayList;

import se.sics.kompics.Event;
import se.sics.kompics.address.Address;

import common.peer.PeerCap;

public class CyclonSample extends Event {
	ArrayList<PeerCap> nodes = new ArrayList<PeerCap>();

	public CyclonSample(ArrayList<PeerCap> nodes) {
		this.nodes = nodes;
	}

	public CyclonSample() {
	}

	public ArrayList<PeerCap> getSample() {
		return this.nodes;
	}

	public ArrayList<Address> getAddresses() {
		ArrayList<Address> addresses = new ArrayList<Address>();

		for (PeerCap pCap : nodes) {
			addresses.add(pCap.address);
		}

		return addresses;
	}
}
