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
import se.sics.kompics.address.Address;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;

public class RmWorker extends ComponentDefinition {
	private static final Logger logger = LoggerFactory
			.getLogger(RmWorker.class);

	Positive<Timer> timerPort = positive(Timer.class);
	Positive<WorkerPort> workerPort = positive(WorkerPort.class);
	Negative<WorkerPort> workerPort2 = negative(WorkerPort.class);

	private AvailableResourcesImpl res = null;
	private Address self;

	private Map<Long, TaskPlaceholder> waitingConfirmation = new HashMap<Long, TaskPlaceholder>();

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
			self = init.getPeerSelf();
			res = init.getAvailableResources();
		}
	};

	Handler<Resources.Reserve> handleReserve = new Handler<Resources.Reserve>() {
		@Override
		public void handle(Resources.Reserve event) {
			System.err.println(System.currentTimeMillis() + " "
					+ event.getTask().getId());
			res.workingQueue.waiting.add(event.getTask());

			pop();
		}
	};

	Handler<Resources.Allocate> handleAllocate = new Handler<Resources.Allocate>() {
		@Override
		public void handle(Resources.Allocate event) {
			if (waitingConfirmation.containsKey(event.referencId)) {

				TaskPlaceholder placeholder = waitingConfirmation
						.get(event.referencId);
				res.release(placeholder.getNumCpus(),
						placeholder.getMemoryInMbs());

				res.allocate(event.getTask().getNumCpus(), event.getTask()
						.getMemoryInMbs());

				res.workingQueue.running.put(event.getTask().getId(),
						event.task);

				runTask(event.task);

			} else
				System.err.println("AAAA");
		}
	};

	Handler<Resources.Cancel> handleCancel = new Handler<Resources.Cancel>() {
		@Override
		public void handle(Resources.Cancel event) {
			if (waitingConfirmation.containsKey(event.referencId)) {

				TaskPlaceholder remove = waitingConfirmation
						.remove(event.referencId);

				res.release(remove.getNumCpus(), remove.getMemoryInMbs());
				res.workingQueue.running.remove(remove.getId());

				System.err.println(self.getIp() + " REMOVED " + remove.getId());

			} else
				System.err.println("CCCC");
		}
	};

	Handler<TaskDone> handleTaskDone = new Handler<TaskDone>() {
		@Override
		public void handle(TaskDone event) {
			Task t = (Task) res.workingQueue.running.remove(event.id);
			if (t == null)
				System.err.println(self.getIp() + " " + event.id);
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
		TaskPlaceholder t = (TaskPlaceholder) res.workingQueue.waiting.peek();
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

	private void getConfirm(TaskPlaceholder t) {
		// here we temporary block resources
		// and ask for confirmation
		res.allocate(t.getNumCpus(), t.getMemoryInMbs());
		waitingConfirmation.put(t.id, t);

		trigger(new Resources.Confirm(t), workerPort);

	}

	private void allocateDirectly(TaskPlaceholder placeholder) {
		// here we allocate resources and start the timers
		res.allocate(placeholder.getNumCpus(), placeholder.getMemoryInMbs());

		res.workingQueue.running.put(placeholder.getId(), placeholder);

		logger.info("Allocated {}", placeholder.getId());

		runTask(placeholder);
	}

	private void runTask(Task t) {
		System.err.println(self.getIp() + " RUNNING " + t.getId() + " ("
				+ res.numFreeCpus + "/" + res.freeMemInMbs + ")");
		t.allocate();
		ScheduleTimeout tout = new ScheduleTimeout(t.getTimeToHoldResource());
		tout.setTimeoutEvent(new TaskDone(tout, t.getId()));
		trigger(tout, timerPort);
	}

}
