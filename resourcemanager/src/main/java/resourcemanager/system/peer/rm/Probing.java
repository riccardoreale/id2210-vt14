package resourcemanager.system.peer.rm;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;

public class Probing {

	public static class Request extends Message {
		private static final long serialVersionUID = 6925804371732048383L;

		protected Request(Address self, Address target) {
			super(self, target);
		}
	}

	public static class Response extends Message {
		private static final long serialVersionUID = 1091630415746168650L;

		protected Response(Request ansTo) {
			super(ansTo.getDestination(), ansTo.getSource());
		}
	}

	public static class Cancel extends Message {
		private static final long serialVersionUID = 3997942241176863428L;

		protected Cancel(Address self, Address target) {
			super(self, target);
		}
	}
}