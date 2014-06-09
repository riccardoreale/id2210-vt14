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
	private final Negative<Signals> servicePort = negative(Signals.class);
	
	public Detector() {
		subscribe(handleInit, control);
		subscribe(handleSubscribe, servicePort);
		subscribe(handleUnsubscribe, servicePort);
		subscribe(handleTimeout, timerPort);
		subscribe(handlePing, networkPort);
	}
	
	private class Info {
		int refcount;
		UUID timeoutId;
		boolean dead;

		Info(UUID timeoutId) {
			this.refcount = 1;
			this.dead = false;
			this.timeoutId = timeoutId;
		}
	}

	private DetectorInit conf = null;
	private Map<Address, Detector.Info> tracked = new HashMap<Address, Detector.Info>();
	
	private UUID setTimer (Address target, long delay) {
		ScheduleTimeout st = new ScheduleTimeout(delay);
		DetectorTimeout tout = new DetectorTimeout(st, target);
		st.setTimeoutEvent(tout);
		trigger(st, timerPort);
		return tout.getTimeoutId();	
	}

	private void sendPing(Address target, boolean ansEx) {
		trigger(new Ping(conf.self, target, ansEx), networkPort);
	}
	
	private Handler<DetectorInit> handleInit = new Handler<DetectorInit>() {
		@Override
		public void handle(DetectorInit event) {
			conf = event;
		}
	};
	
	private Handler<Signals.Subscribe> handleSubscribe = new Handler<Signals.Subscribe>() {
		@Override
		public void handle(Signals.Subscribe event) {
			Info stored = tracked.get(event.ref);
			if (stored == null) {
				sendPing(event.ref, true);
				UUID tid = setTimer(event.ref, conf.pingTimeout);
				stored = new Info(tid);
				tracked.put(event.ref, stored);
			} else {
				assert false : "Currently not enable. Do we need this?";
				stored.refcount ++;
			}
		}
	};

	private Handler<Signals.Unsubscribe> handleUnsubscribe = new Handler<Signals.Unsubscribe>() {
		@Override
		public void handle(Signals.Unsubscribe event) {
			Info stored = tracked.get(event.ref);
			if (stored == null) return;
			if (stored.refcount > 1) {
				assert false : "Currently not enable. Do we need this?";
				stored.refcount --;
			} else {
				trigger(new CancelTimeout(stored.timeoutId), timerPort);
				tracked.remove(event.ref);
			}
		}
	};

	private Handler<DetectorTimeout> handleTimeout = new Handler<DetectorTimeout>() {
		@Override
		public void handle(DetectorTimeout event) {
			Info stored = tracked.get(event.ref);
			assert stored != null;
			
			stored.dead = true;
			sendPing(event.ref, true);
			setTimer(event.ref, conf.pingTimeout);
			trigger(new Signals.Dead(event.ref), servicePort);
		}
	};
	
	private Handler<Ping> handlePing = new Handler<Ping>() {
		@Override
		public void handle(Ping event) {
			Address from = event.getSource();
			Info stored = tracked.get(from);
			if (event.answerExpected) {
				sendPing(from, false); // XXX s/false/stored != null/?
			}
			if (stored != null) {
				if (stored.dead) {
					/* We signaled the peer as dead, so we need to withdraw our claim. No need
					* to re-trigger the ping + timeout, as they are still running. */
					trigger(new Signals.Undead(from), servicePort);
					stored.dead = false;
				} else {
					/* We did not get a timeout yet, so the timer is still active! */
					trigger(new CancelTimeout(stored.timeoutId), timerPort);

					/* Thinking...
					 *
					 *  If the two hosts are pinging each other, here we have twice as much
					 *  pings. I suspect a good way to fix this would be removing the following
					 *  `sendPing` and put `stored != null` in the XXX line above?
					 *
					 *  Keeping it simple for the moment.
					 */
					sendPing(from, true);

					/* FIXME: shall we increase time to measured round-trip time? */
					stored.timeoutId = setTimer(from, conf.pingTimeout);
				}
			}
		}
	};
}
