package main;

public class MainSimulations {

	public static void main(String[] args) throws Throwable {

		// Main.main(new String[] { "true" });

		// Main.main(new String[] { "true", "500000", "1" });

		int numSeeds = 5;
		Long[] seeds = new Long[numSeeds];
		for (int i = 1; i <= numSeeds; i++) {
			seeds[i - 1] = (long) (System.currentTimeMillis() / i);
		}
		for (int load = 1; load <= 10; load += 1) {
			for (Long seed : seeds) {
				Main.main(new String[] { "false", "" + seed,
						"" + (float) load / 10, "1" });
			}
		}

	}

}
