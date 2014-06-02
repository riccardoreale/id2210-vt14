package main;

import simulator.core.DataCenterSimulationMain;

import common.configuration.Configuration;
import common.simulation.scenarios.Scenario;
import common.simulation.scenarios.Scenario1;

public class Main {

	public static void main(String[] args) throws Throwable {
		// TODO - change the random seed, have the user pass it in.

		boolean omniscent = false;
		if (args.length > 0) {
			omniscent = Boolean.parseBoolean(args[0]);
		}
		System.err.printf("Omniscent: %s\n", omniscent);

		long seed = 2000;// System.currentTimeMillis();
		Configuration configuration = new Configuration(seed, omniscent);

		Scenario scenario = new Scenario1();
		scenario.setSeed(seed);
		scenario.getScenario().simulate(DataCenterSimulationMain.class);
	}
}
