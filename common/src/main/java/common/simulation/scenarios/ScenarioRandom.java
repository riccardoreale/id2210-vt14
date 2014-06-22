package common.simulation.scenarios;

import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;

@SuppressWarnings("serial")
public class ScenarioRandom extends Scenario {

	// public static float SYSTEM_LOAD = 0.8f;

	private static final int NODES_FAULTY = 5;

	private static final boolean USE_RANDOM_CPU = true;
	private static final int FIXED_NODE_CPU = 8;
	private static final int MIN_NODE_CPU = 4;
	private static final int MAX_NODE_CPU = 12;
	private static final int NODE_MEMORY = 16; // GB

	private static final int JOBS = 500;


	private static final int MIN_JOB_CPU = 1;
	private static final int MAX_JOB_CPU = 8;

	private static final boolean USE_RANDOM_JOB_MEMORY = false;
	private static final int FIXED_JOB_MEMORY = 1; //GB
	private static final int MIN_JOB_MEMORY = 1;//GB
	private static final int MAX_JOB_MEMORY = 8;//GB

	private static final int MIN_JOB_TIME = 1000;
	private static final int MAX_JOB_TIME = 30000;

	private static final int duration = 300 * 1000;

	private static SimulationScenario scenario;

	public static void generateScenario(final int numNodes) {
		scenario = new SimulationScenario() {
			{

				StochasticProcess process0 = new StochasticProcess() {
					{
						eventInterArrivalTime(constant(100));
						raise(numNodes,
								Operations.peerJoin(),
								uniform(0, Integer.MAX_VALUE),
								USE_RANDOM_CPU ? uniform(MIN_NODE_CPU,
										MAX_NODE_CPU)
										: constant(FIXED_NODE_CPU),
								constant(NODE_MEMORY));
					}
				};

				StochasticProcess process1 = new StochasticProcess() {
					{
						eventInterArrivalTime(exponential(500));
						raise(JOBS,
								Operations.requestResources(),
								uniform(0, Integer.MAX_VALUE),
								uniform(MIN_JOB_CPU, MAX_JOB_CPU),
								USE_RANDOM_JOB_MEMORY ? uniform(MIN_JOB_MEMORY,
										MAX_JOB_MEMORY)
										: constant(FIXED_JOB_MEMORY),
								uniform(MIN_JOB_TIME, MAX_JOB_TIME) // 1 minute
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
				// failPeersProcess.startAfterStartOf(5500, process0);
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
