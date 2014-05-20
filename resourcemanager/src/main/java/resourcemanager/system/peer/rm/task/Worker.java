package resourcemanager.system.peer.rm.task;

import java.util.LinkedList;
import java.util.Queue;

import resourcemanager.system.peer.rm.RmInit;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.timer.Timer;

import common.peer.AvailableResources;

public class Worker extends ComponentDefinition {

	Positive<Timer> timerPort = positive(Timer.class);

	Queue<Task> running = new LinkedList<Task>();
	Queue<Task> waiting = new LinkedList<Task>();
	Queue<Task> done = new LinkedList<Task>();

	public Worker() {
		subscribe(handleInit, control);
	}

	Handler<WorkerInit> handleInit = new Handler<WorkerInit>() {
		@Override
		public void handle(WorkerInit init) {
		}
	};

}
