package resourcemanager.system.peer.rm.task;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import resourcemanager.system.peer.rm.AllocateResources;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;

import common.peer.AvailableResources;

public class RmWorker extends ComponentDefinition {
	private static final Logger logger = LoggerFactory
			.getLogger(RmWorker.class);

	Positive<Timer> timerPort = positive(Timer.class);
	Positive<WorkerPort> workerPort = positive(WorkerPort.class);

	Map<Long, Task> running = new HashMap<Long, Task>();
	Queue<Task> waiting = new LinkedList<Task>();
	Queue<Task> done = new LinkedList<Task>();

	private AvailableResources res = null;

	public RmWorker() {
		subscribe(handleInit, control);
		subscribe(handleAllocateResources, workerPort);
		subscribe(handleTaskDone, timerPort);
	}

	Handler<WorkerInit> handleInit = new Handler<WorkerInit>() {
		@Override
		public void handle(WorkerInit init) {
			res = init.getAvailableResources();
		}
	};

	Handler<AllocateResources> handleAllocateResources = new Handler<AllocateResources>() {
		@Override
		public void handle(AllocateResources event) {
			Task t = new Task(event.getId(), event.getNumCpus(),
					event.getMemoryInMbs(), event.getTimeToHoldResource());
			waiting.add(t);
			pop();
		}
	};

	Handler<TaskDone> handleTaskDone = new Handler<TaskDone>() {
		@Override
		public void handle(TaskDone event) {
			Task t = running.remove(event.id);
			assert t != null;
			t.deallocate();
			res.release(t.getNumCpus(), t.getMemoryInMbs());
			done.add(t);
			logger.info(
					"Done {}, QueueTime={}, TotalTime={}",
					new Object[] {
							t.getId(),
							t.getQueueTime(),
							(float) (t.getTimeToHoldResource())
									/ t.getTotalTime() });

			/* last */
			pop();
		}
	};

	private void pop() {
		Task t = waiting.peek();
		if (t == null)
			return;

		int numCpus = t.getNumCpus();
		int memoryInMbs = t.getMemoryInMbs();
		if (res.isAvailable(numCpus, memoryInMbs)) {
			waiting.poll();
			res.allocate(numCpus, memoryInMbs);
			t.allocate();
			logger.info("Allocated {}", t.getId());
			running.put(t.getId(), t);

			ScheduleTimeout tout = new ScheduleTimeout(
					t.getTimeToHoldResource());
			tout.setTimeoutEvent(new TaskDone(tout, t.getId()));
			trigger(tout, timerPort);
		}
	}

}
