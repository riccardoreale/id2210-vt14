package tman.system.peer.tman;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;

import common.configuration.TManConfiguration;
import common.peer.AvailableResources;
import common.peer.PeerCap;

import cyclon.system.peer.cyclon.CyclonSample;
import cyclon.system.peer.cyclon.CyclonSamplePort;

public final class TMan extends ComponentDefinition {

	private static final Logger log = LoggerFactory.getLogger(TMan.class);

	Negative<TManSamplePort> tmanPort = negative(TManSamplePort.class);
	Positive<CyclonSamplePort> cyclonSamplePort = positive(CyclonSamplePort.class);
	Positive<Network> networkPort = positive(Network.class);
	Positive<Timer> timerPort = positive(Timer.class);
	private long period;
	private Address self;
	private ArrayList<PeerCap> tmanPartners;
	protected ArrayList<PeerCap> cyclonPartners;
	private TManConfiguration tmanConfiguration;
	private Random r;
	private AvailableResources availableResources;

	private int psi = 5;
	private int m = 10;

	public class TManSchedule extends Timeout {

		public TManSchedule(SchedulePeriodicTimeout request) {
			super(request);
		}

		public TManSchedule(ScheduleTimeout request) {
			super(request);
		}
	}

	public TMan() {
		tmanPartners = new ArrayList<PeerCap>();

		subscribe(handleInit, control);
		subscribe(handleRound, timerPort);
		subscribe(handleCyclonSample, cyclonSamplePort);
		subscribe(handleTManPartnersResponse, networkPort);
		subscribe(handleTManPartnersRequest, networkPort);
	}

	Handler<TManInit> handleInit = new Handler<TManInit>() {
		@Override
		public void handle(TManInit init) {
			self = init.getSelf();
			tmanConfiguration = init.getConfiguration();
			period = tmanConfiguration.getPeriod();
			r = new Random(tmanConfiguration.getSeed() * self.getId());
			availableResources = init.getAvailableResources();

			tmanPartners = new ArrayList<PeerCap>();
			cyclonPartners = new ArrayList<PeerCap>();

			SchedulePeriodicTimeout rst = new SchedulePeriodicTimeout(period,
					period);
			rst.setTimeoutEvent(new TManSchedule(rst));
			trigger(rst, timerPort);

		}
	};

	Handler<TManSchedule> handleRound = new Handler<TManSchedule>() {
		@Override
		public void handle(TManSchedule event) {

			if (tmanPartners.isEmpty()) {
				if (cyclonPartners.isEmpty()) {
					// nothing, wait for cyclon samples
					return;
				}
				Set<PeerCap> cyclonSet = new HashSet<PeerCap>(cyclonPartners);
				cyclonSet.remove(self);
				tmanPartners = selectView(generateMyPeerCap(), cyclonSet, m);
			}

			Address p = selectPeer();

			Set<PeerCap> buf = new HashSet<PeerCap>();
			buf.add(generateMyPeerCap());
			buf.addAll(cyclonPartners);
			buf.addAll(tmanPartners);

			ArrayList<PeerCap> selectView = selectView(generateMyPeerCap(),
					buf, m);

			ExchangeMsg.Request tmanViewRequest = new ExchangeMsg.Request(self,
					p, selectView, generateMyPeerCap());
			trigger(tmanViewRequest, networkPort);
			buf.clear();

			// if (self.getIp().toString().equals("/153.20.24.193"))
			// System.err.println(generateMyPeerCap() + " " + tmanPartners);

			// Snapshot.updateTManPartners(self, tmanPartners);
			// Publish sample to connected components

			trigger(new TManSample(tmanPartners), tmanPort);
		}
	};

	protected boolean limit;

	Handler<CyclonSample> handleCyclonSample = new Handler<CyclonSample>() {
		@Override
		public void handle(CyclonSample event) {
			cyclonPartners = event.getSample();

		}
	};

	Handler<ExchangeMsg.Request> handleTManPartnersRequest = new Handler<ExchangeMsg.Request>() {
		@Override
		public void handle(ExchangeMsg.Request event) {

			Set<PeerCap> buf = new HashSet<PeerCap>();
			buf.addAll(cyclonPartners);
			buf.addAll(tmanPartners);
			buf.add(generateMyPeerCap());
			ArrayList<PeerCap> rankedView = selectView(
					event.getSourcePeerCap(), buf, m);

			ExchangeMsg.Response tmanViewResponse = new ExchangeMsg.Response(
					self, event.getSource(), rankedView);
			buf.clear();
			trigger(tmanViewResponse, networkPort);

			buf.addAll(event.getRandomBuffer());
			buf.addAll(tmanPartners);

			// tmanPartners = selectView(generateMyPeerCap(), buf, m);
			tmanPartners = new ArrayList<PeerCap>(buf);

			buf.clear();
		}
	};

	Handler<ExchangeMsg.Response> handleTManPartnersResponse = new Handler<ExchangeMsg.Response>() {
		@Override
		public void handle(ExchangeMsg.Response event) {

			Set<PeerCap> buf = new HashSet<PeerCap>();
			buf.addAll(event.getSelectedBuffer());
			buf.addAll(tmanPartners);

			tmanPartners = new ArrayList<PeerCap>(buf);
		}
	};

	private ArrayList<PeerCap> selectView(PeerCap ref, Set<PeerCap> buf,
			int limit) {
		ArrayList<PeerCap> rankedView = rank(ref, buf);

		if (rankedView.size() > limit)
			rankedView = new ArrayList<PeerCap>(rankedView.subList(0, limit));

		return rankedView;
	}

	protected PeerCap generateMyPeerCap() {
		return new PeerCap(self, availableResources.getTotalCpus(),
				availableResources.getTotalMemory(),
				availableResources.getNumFreeCpus(),
				availableResources.getFreeMemInMbs());
	}

	private ArrayList<PeerCap> rank(PeerCap ref, Set<PeerCap> buf) {
		ArrayList<PeerCap> list = new ArrayList<PeerCap>(buf);
		Collections.sort(list, new GradientResourceComparator(ref));

		return list;
	}

	// TODO - if you call this method with a list of entries, it will
	// return a single node, weighted towards the 'best' node (as defined by
	// ComparatorById) with the temperature controlling the weighting.
	// A temperature of '1.0' will be greedy and always return the best node.
	// A temperature of '0.000001' will return a random node.
	// A temperature of '0.0' will throw a divide by zero exception :)
	// Reference:
	// http://webdocs.cs.ualberta.ca/~sutton/book/2/node4.html
	public Address getSoftMaxAddress(List<Address> entries) {
		Collections.sort(entries, new ComparatorById(self));

		double rnd = r.nextDouble();
		double total = 0.0d;
		double[] values = new double[entries.size()];
		int j = entries.size() + 1;
		for (int i = 0; i < entries.size(); i++) {
			// get inverse of values - lowest have highest value.
			double val = j;
			j--;
			values[i] = Math.exp(val / tmanConfiguration.getTemperature());
			total += values[i];
		}

		for (int i = 0; i < values.length; i++) {
			if (i != 0) {
				values[i] += values[i - 1];
			}
			// normalise the probability for this entry
			double normalisedUtility = values[i] / total;
			if (normalisedUtility >= rnd) {
				return entries.get(i);
			}
		}
		return entries.get(entries.size() - 1);
	}

	private Address selectPeer() {
		if (tmanPartners.isEmpty()) {
			return null;
		} else if (tmanPartners.size() == 1) {
			return tmanPartners.get(0).getAddress();
		} else {

			ArrayList<PeerCap> rankedView = selectView(generateMyPeerCap(),
					new HashSet<PeerCap>(tmanPartners), psi);

			int q = r.nextInt(rankedView.size());

			Address peerSelected = rankedView.get(q).getAddress();

			return peerSelected;
		}
	}

}
