package tman.system.peer.tman;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;

import common.peer.PeerCap;
import common.utils.SoftMax;

import cyclon.system.peer.cyclon.PeerDescriptor;

public class GradientResourceComparator implements Comparator<PeerDescriptor> {

	private static final boolean USE_GREEDY = true;
	private static final double TEMPERATURE_GRADIENT = 2;

	private int valueRef;
	private SoftMax softMax;

	public GradientResourceComparator(PeerCap ref,
			ArrayList<PeerDescriptor> list, Random r) {
		valueRef = ref.getUtilityFunction();

		ArrayList<PeerCap> pCapList = new ArrayList<PeerCap>();
		for (PeerDescriptor pDescriptor : list) {
			pCapList.add(pDescriptor.getPeerCap());
		}
		softMax = new SoftMax(pCapList, TEMPERATURE_GRADIENT, r);
	}

	/*
	 * very basic, for now applies greedy function only to available cpu
	 */
	@Override
	public int compare(PeerDescriptor arg0, PeerDescriptor arg1) {
		int value0 = arg0.getPeerCap().getUtilityFunction();
		int value1 = arg1.getPeerCap().getUtilityFunction();

		if (USE_GREEDY) {
			if (value0 > valueRef
					&& value1 < valueRef
					|| Math.abs(value0 - valueRef) < Math
							.abs(value1 - valueRef))
				return 0;
			else
				return 1;
		} else {
			PeerCap picked = softMax.pickPeer(arg0.getPeerCap(),
					arg1.getPeerCap());
			if (arg0.getPeerCap().equals(picked))
				return 0;
			else
				return 1;
		}
	}
}
