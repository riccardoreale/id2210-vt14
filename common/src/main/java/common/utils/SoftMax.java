package common.utils;

import java.util.List;
import java.util.Random;

import common.peer.PeerCap;

public class SoftMax {

	private final Random random;
	private double[] values;
	private List<PeerCap> pList;
	private int elements;
	private double[] probabilities;

	public SoftMax(List<PeerCap> p, double temperature, Random r) {
		this.random = r;
		this.elements = p.size();
		this.pList = p;
		values = new double[elements];

		// Collections.shuffle(p, random);
		initProbabilities(p, temperature);
	}

	private void initProbabilities(List<PeerCap> p, double temperature) {
		for (int i = 0; i < elements; i++) {
			values[i] = p.get(i).getUtilityFunction();
		}
		double z = 0;
		for (double value : values) {
			z += Math.exp(value / temperature);
		}

		probabilities = new double[elements];
		for (int i = 0; i < values.length; i++) {
			double value = values[i];
			probabilities[i] = Math.exp(value / temperature) / z;
		}
	}

	public PeerCap pickPeer() {
		return pList.get(categoricalDraw(probabilities, 1));
	}

	public PeerCap pickPeer(PeerCap arg0, PeerCap arg1) {
		double totalProbability = 0;
		double subListProb[] = new double[2];
		PeerCap[] subList = new PeerCap[2];

		int c = 0;
		for (int i = 0; i < pList.size() || c < 2; i++) {
			PeerCap peerCap = pList.get(i);
			if (peerCap.equals(arg0) || peerCap.equals(arg1)) {

				double probability = probabilities[i];
				totalProbability += probability;

				subListProb[c] = probability;
				subList[c] = peerCap;
				c++;
			}
		}

		return subList[categoricalDraw(subListProb, totalProbability)];
	}

	private int categoricalDraw(double[] probabilities, double totalProbability) {
		double rand = random.nextDouble() * totalProbability;
		double cumulativeProbability = 0;
		for (int i = 0; i < probabilities.length; i++) {
			double probability = probabilities[i];
			cumulativeProbability += probability;
			if (cumulativeProbability > rand) {
				return i;
			}
		}
		return probabilities.length - 1;
	}
}