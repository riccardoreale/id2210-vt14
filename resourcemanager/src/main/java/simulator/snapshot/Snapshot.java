package simulator.snapshot;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.math.stat.descriptive.rank.Percentile;

import resourcemanager.system.peer.rm.task.AvailableResourcesImpl;
import resourcemanager.system.peer.rm.task.Task;
import se.sics.kompics.address.Address;

import common.peer.AvailableResources;

public class Snapshot {

	private static ConcurrentHashMap<Address, PeerInfo> peers = new ConcurrentHashMap<Address, PeerInfo>();
	private static int counter = 0;
	private static String FILENAME = "search.out";
	private static int probes;
	private static float load;

	public static void init(int numProbes, float loadF) {
		probes = numProbes;
		load = loadF;
		peers.clear();
	}

	public static void addPeer(Address address,
			AvailableResources availableResources) {
		peers.put(address, new PeerInfo(availableResources));
	}

	public static void removePeer(Address address) {
		peers.remove(address);
	}

	public static void updateNeighbours(Address address,
			ArrayList<Address> partners) {
		PeerInfo peerInfo = peers.get(address);

		if (peerInfo == null) {
			return;
		}

		peerInfo.setNeighbours(partners);
	}

	public static void report() {
		String str = new String();
		str += "current time: " + counter++ + "\n";
		str += reportNetworkState();
		str += reportDetails();
		str += "###\n";

		System.out.println(str);
		FileIO.append(str, FILENAME);
	}

	public static void evaluate() {
		long totalTime = 0;
		long totalQueueTime = 0;
		long totalDone = 0;
		ArrayList<Double> queueTimes = new ArrayList<Double>();

		for (PeerInfo p : peers.values()) {
			AvailableResourcesImpl res = (AvailableResourcesImpl) p
					.getAvailableResources();

			List<Task> done = res.getWorkingQueue().getDone();
			totalDone += done.size();
			for (Task task : done) {
				totalTime += task.getTotalTime();
				totalQueueTime += task.getQueueTime();
				queueTimes.add((double) task.getQueueTime());
			}
		}

		double ratio = (double) totalQueueTime / totalTime;
		double averageQueue = Math.round((double) totalQueueTime / totalDone);
		Percentile p = new Percentile(99);
		double[] values = new double[queueTimes.size()];
		for (int i = 0; i < queueTimes.size(); i++) {
			values[i] = queueTimes.get(i);
		}
		// System.err.println(Arrays.toString(values));
		double percentile99 = Math.round(p.evaluate(values));

		// String roundResults = load + "\t" + probes + "\t" + totalTime + "\t"
		// + totalQueueTime + "\t" + ratio + "\t" + averageQueue;

		String roundResults = load + "\t" + probes + "\t" + averageQueue + "\t"
				+ percentile99;

		System.err.println(roundResults);

		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(
					new FileWriter("out.txt", true)));
			out.println(roundResults);
			out.close();
		} catch (IOException e) {
			// exception handling left as an exercise for the reader
		}

	}

	private static String reportNetworkState() {
		String str = "---\n";
		int totalNumOfPeers = peers.size();
		str += "total number of peers: " + totalNumOfPeers + "\n";

		return str;
	}

	private static String reportDetails() {
		String str = "---\n";
		int maxFreeCpus = 0;
		int minFreeCpus = Integer.MAX_VALUE;
		int maxFreeMemInMb = 0;
		int minFreeMemInMb = Integer.MAX_VALUE;
		for (PeerInfo p : peers.values()) {
			if (p.getNumFreeCpus() > maxFreeCpus) {
				maxFreeCpus = p.getNumFreeCpus();
			}
			if (p.getNumFreeCpus() < minFreeCpus) {
				minFreeCpus = p.getNumFreeCpus();
			}
			if (p.getFreeMemInMbs() > maxFreeMemInMb) {
				maxFreeMemInMb = p.getFreeMemInMbs();
			}
			if (p.getFreeMemInMbs() < minFreeMemInMb) {
				minFreeMemInMb = p.getFreeMemInMbs();
			}
		}
		str += "Peer with max num of free cpus: " + maxFreeCpus + "\n";
		str += "Peer with min num of free cpus: " + minFreeCpus + "\n";
		str += "Peer with max amount of free mem in MB: " + maxFreeMemInMb
				+ "\n";
		str += "Peer with min amount of free mem in MB: " + minFreeMemInMb
				+ "\n";

		return str;
	}
}
