package tman.system.peer.tman;

import java.util.Comparator;

import common.peer.PeerCap;

public class GradientResourceComparator implements Comparator<PeerCap> {

	private PeerCap ref;

	public GradientResourceComparator(PeerCap ref) {
		this.ref = ref;
	}

	@Override
	public int compare(PeerCap arg0, PeerCap arg1) {
		if (arg0.getAvailableCpu() > ref.getAvailableCpu()
				&& arg1.getAvailableCpu() < ref.getAvailableCpu()
				|| Math.abs(arg0.getAvailableCpu() - ref.getAvailableCpu()) < Math
						.abs(arg1.getAvailableCpu() - ref.getAvailableCpu()))
			return 0;
		else
			return 1;
	}
}
