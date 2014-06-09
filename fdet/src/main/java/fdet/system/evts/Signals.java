package fdet.system.evts;

import se.sics.kompics.Event;
import se.sics.kompics.PortType;
import se.sics.kompics.address.Address;

public class Signals extends PortType {
	
	{
		positive(Dead.class);
		positive(Undead.class);
		negative(Subscribe.class);
		negative(Unsubscribe.class);
	}
	
	private static class Base extends Event {
		public final Address ref;
		private Base(Address ref) {
			this.ref = ref;
		}
	}

	public static class Dead extends Base {
		public Dead(Address ref) {
			super(ref);
		}
	}
	
	public static class Undead extends Base {
		public Undead(Address ref) {
			super(ref);
		}
	}

	public static class Subscribe extends Base {
		public Subscribe(Address ref) {
			super(ref);
		}
	}

	public static class Unsubscribe extends Base {
		public Unsubscribe(Address ref) {
			super(ref);
		}
	}

}
