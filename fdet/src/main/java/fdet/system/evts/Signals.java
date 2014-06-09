package fdet.system.evts;

import se.sics.kompics.Event;
import se.sics.kompics.PortType;
import se.sics.kompics.address.Address;

public class Signals extends PortType {
	
	{
		positive(Dead.class);
		positive(Undead.class);
	}
	
	public static class Info extends Event {
		final Address ref;
		Info(Address ref) {
			this.ref = ref;
		}
	}

	public static class Dead extends Info {
		Dead(Address ref) {
			super(ref);
		}
	}
	
	public static class Undead extends Info {
		Undead(Address ref) {
			super(ref);
		}
	}

}
