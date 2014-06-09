package fdet.system;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import fdet.system.evts.DetectorInit;
import fdet.system.evts.DetectorTimeout;
import fdet.system.evts.Ping;
import fdet.system.evts.Signals;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;

public class Detector extends ComponentDefinition {
	
	private final Positive<Network> networkPort = positive(Network.class);
	private final Positive<Timer> timerPort = positive(Timer.class); 
	private final Negative<Signals> infoPort = negative(Signals.class);
	
	public Detector() {
		subscribe(handleInit, control);
		subscribe(handleTimeout, timerPort);
		subscribe(handlePing, networkPort);
	}
	
	private class Info {
		int refcount;
		UUID timeoutId;

		Info() {
			this.refcount = 1;
		}
	}

	private DetectorInit conf = null;
	private Map<Address, Detector.Info> tracked = new HashMap<Address, Detector.Info>();
	
	public void subscribe(Address target) {
		Info stored = tracked.get(target);
		if (stored == null) {
			stored = new Info();
			tracked.put(target, stored);
			sendPing(target, conf.minTimeout);
		} else {
			stored.refcount ++;
		}
	}
	
	public boolean unsubscribe (Address target) {
		Info stored = tracked.get(target);
		if (stored == null) return false;
		if (stored.refcount > 1) {
			stored.refcount --;
			return false;
		}
		trigger(new CancelTimeout(stored.timeoutId), timerPort);
		tracked.remove(target);
		return true;
	}
	
	private void sendPing(Address target, long delay) {
		boolean ansEx = delay >= 0;
		trigger(new Ping(conf.self, target, ansEx), networkPort);
		if (ansEx) {
			ScheduleTimeout st = new ScheduleTimeout(delay);
			st.setTimeoutEvent(new DetectorTimeout(st, target));
			trigger(st, timerPort);
		}
	}
	
	private void sendPing(Address target) {
		sendPing(target, -1);
	}
	
	private Handler<DetectorInit> handleInit = new Handler<DetectorInit>() {
		@Override
		public void handle(DetectorInit event) {
			conf = event;
		}
	};
	
	private Handler<DetectorTimeout> handleTimeout = new Handler<DetectorTimeout>() {
		@Override
		public void handle(DetectorTimeout event) {
			Info stored = tracked.get(event.ref);
			assert stored != null;
			
			/* TODO: signal dead, reschedule timeout (longer?) */
		}
	};
	
	private Handler<Ping> handlePing = new Handler<Ping>() {
		@Override
		public void handle(Ping event) {
			Address from = event.getSource();
			Info stored = tracked.get(from);
			if (event.answerExpected) {
				sendPing(from);
			}
			if (stored != null) {
				trigger(new CancelTimeout(stored.timeoutId), timerPort);
			}
		}
	};
}
