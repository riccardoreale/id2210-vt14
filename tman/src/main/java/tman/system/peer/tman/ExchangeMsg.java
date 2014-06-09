package tman.system.peer.tman;

import java.util.ArrayList;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

import common.peer.PeerCap;

public class ExchangeMsg {

	public static class Request extends Message {

		private static final long serialVersionUID = 8493601671018888143L;
		private final ArrayList<PeerCap> randomBuffer;
		private PeerCap sourcePeerCap;

		public Request(Address source, Address destination,
				ArrayList<PeerCap> randomBuffer, PeerCap sourcePeerCap) {
			super(source, destination);
			this.randomBuffer = randomBuffer;
			this.sourcePeerCap = sourcePeerCap;
		}

		public ArrayList<PeerCap> getRandomBuffer() {
			return randomBuffer;
		}

		public PeerCap getSourcePeerCap() {
			return sourcePeerCap;
		}
	}

	public static class Response extends Message {

		private static final long serialVersionUID = -5022051054665787770L;
		private final ArrayList<PeerCap> selectedBuffer;

		public Response(Address source, Address destination,
				ArrayList<PeerCap> selectedBuffer) {
			super(source, destination);
			this.selectedBuffer = selectedBuffer;
		}

		public ArrayList<PeerCap> getSelectedBuffer() {
			return selectedBuffer;
		}

	}

	public static class RequestTimeout extends Timeout {

		private final Address peer;

		public RequestTimeout(ScheduleTimeout request, Address peer) {
			super(request);
			this.peer = peer;
		}

		public Address getPeer() {
			return peer;
		}
	}
}