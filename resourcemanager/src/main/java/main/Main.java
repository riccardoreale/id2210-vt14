package main;

import simulator.core.DataCenterSimulationMain;

import common.configuration.Configuration;
import common.simulation.scenarios.Scenario;
import common.simulation.scenarios.Scenario1;

public class Main {

	public static void main(String[] args) throws Throwable {
		// TODO - change the random seed, have the user pass it in.

		boolean omniscent = Boolean.parseBoolean(args[0]);
		long seed = Long.parseLong(args[1]);
		float load = Float.parseFloat(args[2]);
		int probes = Integer.parseInt(args[3]);

		Configuration configuration = new Configuration(seed, omniscent,
				probes, load);

		Scenario1.generateScenario(load);

		Scenario scenario = new Scenario1();
		scenario.setSeed(seed);
		scenario.getScenario().simulate(DataCenterSimulationMain.class);
	}
}
