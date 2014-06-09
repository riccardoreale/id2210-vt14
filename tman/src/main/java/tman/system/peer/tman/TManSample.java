package tman.system.peer.tman;

import java.util.ArrayList;

import se.sics.kompics.Event;

import common.peer.PeerCap;

public class TManSample extends Event {
	ArrayList<PeerCap> partners = new ArrayList<PeerCap>();

	public TManSample(ArrayList<PeerCap> partners) {
		this.partners = partners;
	}

	public TManSample() {
	}

	public ArrayList<PeerCap> getSample() {
		return this.partners;
	}
}
