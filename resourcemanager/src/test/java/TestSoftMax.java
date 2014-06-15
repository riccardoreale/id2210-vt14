import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import se.sics.kompics.address.Address;
import tman.system.peer.tman.GradientResourceComparator;

import common.peer.PeerCap;
import common.utils.SoftMax;

import cyclon.system.peer.cyclon.PeerDescriptor;

public class TestSoftMax {

	private static final double TEMPERATURE = 0.5;
	private ArrayList<PeerCap> p;
	private ArrayList<PeerDescriptor> pd;

	@Before
	public void generatePeerCaps() throws UnknownHostException {
		p = new ArrayList<PeerCap>();
		pd = new ArrayList<PeerDescriptor>();

		for (int i = 0; i < 20; i++) {
			PeerCap peerCap = new PeerCap(new Address(
					Inet4Address.getByName("192.168.1." + i), 8000, i), 8, 20,
					i, i, 0);
			p.add(peerCap);
			pd.add(new PeerDescriptor(peerCap));

		}
	}

	@Test
	public void test1() throws UnknownHostException {

		SoftMax m = new SoftMax(p, TEMPERATURE, new Random(0));

		int results[] = new int[p.size()];
		for (int i = 0; i < 1000; i++) {
			PeerCap selectPeer = m.pickPeer();
			results[selectPeer.getUtilityFunction()]++;
		}

		for (int i = 0; i < results.length; i++) {

			System.err.println(i + "\t" + results[i]);
		}
	}

	@Test
	public void test2() throws UnknownHostException {

		SoftMax m = new SoftMax(p, TEMPERATURE, new Random(0));

		HashMap<PeerCap, Integer> results = new HashMap<PeerCap, Integer>();
		PeerCap p0 = p.get(0);
		PeerCap p1 = p.get(1);
		results.put(p0, 0);
		results.put(p1, 0);

		for (int i = 0; i < 1000; i++) {
			PeerCap selectPeer = m.pickPeer(p0, p1);
			results.put(selectPeer, results.get(selectPeer) + 1);
		}

		for (Entry<PeerCap, Integer> i : results.entrySet()) {
			System.err.println(i.getKey() + " " + i.getValue());
		}
	}

	@Test
	public void test3() throws UnknownHostException {

		Collections.sort(pd, new GradientResourceComparator(p.get(10), pd,
				new Random(0)));

		System.err.println(pd);
	}

}
