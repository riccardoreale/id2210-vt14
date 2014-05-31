package cyclon.system.peer.cyclon;

import java.util.ArrayList;

import se.sics.kompics.Event;

import common.peer.PeerCap;

public class CyclonPartnersResponse extends Event {
	ArrayList<PeerCap> partners = new ArrayList<PeerCap>();

	public CyclonPartnersResponse(ArrayList<PeerCap> partners) {
		this.partners = partners;
	}

	public CyclonPartnersResponse() {
	}

	public ArrayList<PeerCap> getPartners() {
		return this.partners;
	}
}
