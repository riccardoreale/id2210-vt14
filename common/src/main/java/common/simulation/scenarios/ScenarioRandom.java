package common.simulation.scenarios;

import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;

@SuppressWarnings("serial")
public class ScenarioRandom extends Scenario {

	// public static float SYSTEM_LOAD = 0.8f;

	private static final int NODES = 20;
	private static final int NODES_FAULTY = 5;
	private static final int NODE_MEMORY = 12000;
	private static final int NODE_CPU = 8;

	private static final int JOBS = 1000;
	private static final int JOB_MEMORY = 2000;
	private static final int JOB_CPU = 2;
	private static final int JOB_TIME = 10000;

	private static final int duration = 300 * 1000;

	private static SimulationScenario scenario;

	public static void generateScenario(final float load) {
		scenario = new SimulationScenario() {
			{
//				final int interArrival = (int) (1000 / (((NODES * NODE_CPU) / JOB_CPU) / (JOB_TIME / 1000)) / load);
				StochasticProcess process0 = new StochasticProcess() {
					{
						eventInterArrivalTime(constant(10));
						raise(NODES, Operations.peerJoin(),
								uniform(0, Integer.MAX_VALUE),
								constant(NODE_CPU), constant(NODE_MEMORY));
					}
				};

				StochasticProcess process1 = new StochasticProcess() {
					{
						eventInterArrivalTime(exponential(100));
						raise(JOBS, Operations.requestResources(),
								uniform(0, Integer.MAX_VALUE),
								uniform(1, 8), uniform(1000, 12000),
								exponential(5000) // 1 minute
						);
					}
				};

				// TODO - not used yet
				StochasticProcess failPeersProcess = new StochasticProcess() {
					{
						eventInterArrivalTime(constant(100));
						raise(NODES_FAULTY, Operations.peerFail,
								uniform(0, Integer.MAX_VALUE));
					}
				};

				StochasticProcess evaluate = new StochasticProcess() {
					{
						raise(1, Operations.evaluate);
					}
				};

				StochasticProcess terminateProcess = new StochasticProcess() {
					{
						eventInterArrivalTime(constant(100));
						raise(1, Operations.terminate);
					}
				};
				process0.start();
				process1.startAfterTerminationOf(5500, process0);
//				failPeersProcess.startAfterStartOf(5500, process0);
				evaluate.startAfterTerminationOf(duration, process1);
				terminateProcess.startAfterTerminationOf(duration, process1);
			}
		};
	}

	// -------------------------------------------------------------------
	public ScenarioRandom() {
		super(scenario);
	}
}
