package main;

import simulator.core.DataCenterSimulationMain;
import common.configuration.Configuration;
import common.simulation.scenarios.Scenario;
import common.simulation.scenarios.ScenarioLoad;
import common.simulation.scenarios.ScenarioRandom;

public class MainRandom {

	public static void main(String[] args) throws Throwable {
		// TODO - change the random seed, have the user pass it in.

		boolean omniscent = Boolean.parseBoolean(args[0]);
		long seed = Long.parseLong(args[1]);
		int numPeers = Integer.parseInt(args[2]);
		int probes = Integer.parseInt(args[3]);
		final long fdetTimeout = 3000;

		Configuration configuration = new Configuration(seed, omniscent,
				probes, numPeers, fdetTimeout);

		ScenarioRandom.generateScenario(numPeers);

		Scenario scenario = new ScenarioRandom();
		scenario.setSeed(seed);
		ScenarioRandom.generateScenario(numPeers);
		scenario.getScenario().simulate(DataCenterSimulationMain.class);

	}
}
