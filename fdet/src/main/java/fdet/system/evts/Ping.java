package fdet.system.evts;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;

public class Ping extends Message {
	private static final long serialVersionUID = -1670333091863044326L;
	public final boolean answerExpected;

	public Ping(Address source, Address destination, boolean answerExpected) {
		super(source, destination);
		this.answerExpected = answerExpected;
	}
}
