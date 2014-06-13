package resourcemanager.system.peer.rm.task;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import resourcemanager.system.peer.rm.Resources;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;

public class RmWorker extends ComponentDefinition {
	private static final Logger log = LoggerFactory.getLogger(RmWorker.class);

	Positive<Timer> timerPort = positive(Timer.class);
	Positive<WorkerPort> workerPort = positive(WorkerPort.class);

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

	private String getId() {
		return "[" + res.getNumFreeCpus() + "/"
				+ res.getWorkingQueue().running.size() + "/"
				+ res.getWorkingQueue().waiting.size() + "/"
				+ waitingConfirmation.size() + " " + self.getIp() + "]";
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
			res.workingQueue.waiting.add(event.getTask());

			pop();
		}
	};

	Handler<Resources.Allocate> handleAllocate = new Handler<Resources.Allocate>() {
		@Override
		public void handle(Resources.Allocate event) {
			if (waitingConfirmation.containsKey(event.referenceId)) {

				TaskPlaceholder placeholder = waitingConfirmation
						.remove(event.referenceId);
				res.release(placeholder.getNumCpus(),
						placeholder.getMemoryInMbs());

				res.allocate(event.getTask().getNumCpus(), event.getTask()
						.getMemoryInMbs());

				res.workingQueue.running.put(event.getTask().getId(),
						event.task);

				runTask(event.task);

			} else
				log.warn("AAAA");
		}
	};

	Handler<Resources.Cancel> handleCancel = new Handler<Resources.Cancel>() {
		@Override
		public void handle(Resources.Cancel event) {
			if (waitingConfirmation.containsKey(event.referenceId)) {

				TaskPlaceholder remove = waitingConfirmation
						.remove(event.referenceId);

				res.release(remove.getNumCpus(), remove.getMemoryInMbs());
				// res.workingQueue.running.remove(remove.getId());

				log.debug(getId() + " REMOVED " + remove.getId());

				pop();

			} else
				log.warn("CCCC");
		}
	};

	Handler<TaskDone> handleTaskDone = new Handler<TaskDone>() {
		@Override
		public void handle(TaskDone event) {
			Task t = (Task) res.workingQueue.running.remove(event.referenceId);
			if (t == null)
				log.error(getId() + " " + event.referenceId);
			assert t != null;
			t.deallocate();
			res.release(t.getNumCpus(), t.getMemoryInMbs());
			res.workingQueue.getDone().add(t);
			log.info(
					"{} Done {}, QueueTime={}, TotalTime={}",
					new Object[] {
							getId(),
							t.getId(),
							t.getQueueTime(),
							(float) (t.getTimeToHoldResource())
									/ t.getTotalTime() });
			trigger(new Resources.Completed(t.getId()), workerPort);

			/* last */
			pop();
		}
	};

	private void pop() {
		TaskPlaceholder t = (TaskPlaceholder) res.workingQueue.waiting.peek();
		if (t == null) {
			// FIXME: probably this could be a single assertion: !(a && b) || c
			if (res.workingQueue.running.size() == 0)
				if (waitingConfirmation.size() == 0)
					assert res.getNumFreeCpus() == res.getTotalCpus();
			return;
		}
		if (res.isAvailable(t.getNumCpus(), t.getMemoryInMbs())) {
			res.workingQueue.waiting.poll();

			if (t.isExecuteDirectly())
				allocateDirectly(t);
			else
				getConfirm(t);
		} else {
			// FIXME: check when inserting into workingQueue, not here.
			assert false : "This should never happen";
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

		log.info(getId() + " Allocated {}", placeholder.getId());

		runTask(placeholder);
	}

	private void runTask(Task t) {
		log.debug(getId() + " RUNNING " + t.getId() + " (" + res.numFreeCpus
				+ "/" + res.freeMemInMbs + ")");
		t.allocate();
		ScheduleTimeout tout = new ScheduleTimeout(t.getTimeToHoldResource());
		tout.setTimeoutEvent(new TaskDone(tout, t.getId()));
		trigger(tout, timerPort);
	}

}
