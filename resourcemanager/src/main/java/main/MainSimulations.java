package main;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class MainSimulations {

	private final static boolean USE_ORACLE = false;
	private final static int PROBES = 2;

	private final static int numSeeds = 5;
	private final static double[] loads = new double[] { //
	0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.85, 0.9, 0.95, 1
	// 0.8, 0.85, 0.9, 0.95, 1
	};

	public static void main(String[] args) throws Throwable {

		cleanUp();

		Long[] seeds = new Long[numSeeds];
		for (int i = 1; i <= numSeeds; i++) {
			seeds[i - 1] = (long) (44444L / i);
		}
		for (double load : loads) {
			for (Long seed : seeds) {
				Main.main(new String[] { String.valueOf(USE_ORACLE), "" + seed,
						"" + load, String.valueOf(PROBES) });
			}

			averageData();
		}

	}

	private static void cleanUp() {
		try {

			File f = new File("out.txt");
			f.delete();
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
	}

	private static void averageData() {
		try {

			File f = new File("out.txt");
			FileInputStream fstream = new FileInputStream(f);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));

			String strLine;

			String load = null;
			String probes = null;
			double totAverage = 0;
			double totPercentile = 0;
			int lines = 0;
			// Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				lines++;
				String[] split = strLine.split("\t");
				load = split[0];
				probes = split[1];
				totAverage += Double.parseDouble(split[2]);
				totPercentile += Double.parseDouble(split[3]);
			}
			double averageAverage = Math.round(totAverage / lines);
			double averagePercentile = Math.round(totPercentile / lines);
			System.err.println(load + "\t" + probes + "\t" + averageAverage
					+ "\t" + averagePercentile);
			// Close the input stream
			in.close();
			f.delete();
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
	}
}
