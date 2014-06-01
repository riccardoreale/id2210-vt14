package resourcemanager.system.peer.rm.task;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import resourcemanager.system.peer.rm.Resources;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;

public class RmWorker extends ComponentDefinition {
	private static final Logger logger = LoggerFactory
			.getLogger(RmWorker.class);

	Positive<Timer> timerPort = positive(Timer.class);
	Positive<WorkerPort> workerPort = positive(WorkerPort.class);
	Negative<WorkerPort> workerPort2 = negative(WorkerPort.class);

	private AvailableResourcesImpl res = null;

	private Map<Long, RmTask> waitingConfirmation = new HashMap<Long, RmTask>();

	public RmWorker() {
		subscribe(handleInit, control);
		subscribe(handleReserve, workerPort);
		subscribe(handleAllocate, workerPort);
		subscribe(handleCancel, workerPort);
		subscribe(handleTaskDone, timerPort);
	}

	Handler<WorkerInit> handleInit = new Handler<WorkerInit>() {
		@Override
		public void handle(WorkerInit init) {
			res = init.getAvailableResources();
		}
	};

	Handler<Resources.Reserve> handleReserve = new Handler<Resources.Reserve>() {
		@Override
		public void handle(Resources.Reserve event) {
			RmTask t = new RmTask(event.getId(), event.getNumCpus(),
					event.getMemoryInMbs(), 0);
			t.taskMaster = event.taskMaster;
			res.workingQueue.waiting.add(t);
			t.queue();

			pop();
		}
	};

	Handler<Resources.Allocate> handleAllocate = new Handler<Resources.Allocate>() {
		@Override
		public void handle(Resources.Allocate event) {
			if (waitingConfirmation.containsKey(event.referencId)) {

				if (event.task.getId() == event.referencId) {

					RmTask toRun = waitingConfirmation.get(event.referencId);
					toRun.timeToHoldResource = event.task.timeToHoldResource;
					runTask(toRun);
				} else
					System.err.println("BBBB ");
			} else
				System.err.println("AAAA");
		}
	};

	Handler<Resources.Cancel> handleCancel = new Handler<Resources.Cancel>() {
		@Override
		public void handle(Resources.Cancel event) {
			if (waitingConfirmation.containsKey(event.referencId)) {

				RmTask remove = waitingConfirmation.remove(event.referencId);

				res.release(remove.getNumCpus(), remove.getMemoryInMbs());
				res.workingQueue.running.remove(remove.getId());

				System.err.println("REMOVED " + remove.getId());

			} else
				System.err.println("CCCC");
		}
	};

	Handler<TaskDone> handleTaskDone = new Handler<TaskDone>() {
		@Override
		public void handle(TaskDone event) {
			RmTask t = (RmTask) res.workingQueue.running.remove(event.id);
			assert t != null;
			t.deallocate();
			res.release(t.getNumCpus(), t.getMemoryInMbs());
			res.workingQueue.done.add(t);
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
		RmTask t = (RmTask) res.workingQueue.waiting.peek();
		if (t == null)
			return;

		if (res.isAvailable(t.getNumCpus(), t.getMemoryInMbs())) {
			res.workingQueue.waiting.poll();
			if (t.isExecuteDirectly())
				allocateDirectly(t);
			else
				getConfirm(t);
		}
	}

	private void getConfirm(RmTask t) {
		// here we temporary block resources
		// and ask for confirmation
		res.allocate(t.getNumCpus(), t.getMemoryInMbs());
		res.workingQueue.running.put(t.getId(), t);
		waitingConfirmation.put(t.id, t);

		// TODO ask for confirmation
		trigger(new Resources.Confirm(t, t.taskMaster), workerPort);

	}

	private void allocateDirectly(RmTask t) {
		// here we allocate resources and start the timers
		res.allocate(t.getNumCpus(), t.getMemoryInMbs());
		res.workingQueue.running.put(t.getId(), t);

		logger.info("Allocated {}", t.getId());

		runTask(t);
	}

	private void runTask(RmTask t) {
		System.err.println("RUNNING " + t.getId());
		t.allocate();
		ScheduleTimeout tout = new ScheduleTimeout(t.getTimeToHoldResource());
		tout.setTimeoutEvent(new TaskDone(tout, t.getId()));
		trigger(tout, timerPort);
	}

}
