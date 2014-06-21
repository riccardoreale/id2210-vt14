package main;

import simulator.core.DataCenterSimulationMain;

import common.configuration.Configuration;
import common.simulation.scenarios.Scenario;
import common.simulation.scenarios.ScenarioLoad;

public class MainLoad {

	public static void main(String[] args) throws Throwable {
		// TODO - change the random seed, have the user pass it in.

		boolean omniscent = Boolean.parseBoolean(args[0]);
		long seed = Long.parseLong(args[1]);
		float load = Float.parseFloat(args[2]);
		int probes = Integer.parseInt(args[3]);
		final long fdetTimeout = 3000;

		Configuration configuration = new Configuration(seed, omniscent,
				probes, load, fdetTimeout);

		ScenarioLoad.generateScenario(load);

		Scenario scenario = new ScenarioLoad();
		scenario.setSeed(seed);
		ScenarioLoad.generateScenario(load);
		scenario.getScenario().simulate(DataCenterSimulationMain.class);

	}
}
