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

	public class ObjectId {

		public String toString() {
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("[");
			stringBuilder.append(res.getNumFreeCpus());
			stringBuilder.append("/");
			stringBuilder.append(res.getWorkingQueue().running.size());
			stringBuilder.append("/");
			stringBuilder.append(res.getWorkingQueue().waiting.size());
			stringBuilder.append("/");
			stringBuilder.append(waitingConfirmation.size());
			stringBuilder.append(" ");
			stringBuilder.append(self.getIp());
			stringBuilder.append("]");

			return stringBuilder.toString();
		}

	}

	private static final Logger log = LoggerFactory.getLogger(RmWorker.class);

	Positive<Timer> timerPort = positive(Timer.class);
	Positive<WorkerPort> workerPort = positive(WorkerPort.class);
	Negative<WorkerPort> workerPort2 = negative(WorkerPort.class);

	private AvailableResourcesImpl res = null;
	private Address self;
	private ObjectId selfId = new ObjectId();

	private Map<Long, TaskPlaceholder> waitingConfirmation = new HashMap<Long, TaskPlaceholder>();

	public RmWorker() {
		subscribe(handleInit, control);
		subscribe(handleReserve, workerPort);
		subscribe(handleAllocate, workerPort);
		subscribe(handleCancel, workerPort);
		subscribe(handleTaskDone, timerPort);
	}

	private ObjectId getId() {
		return selfId;
	}

	Handler<WorkerInit> handleInit = new Handler<WorkerInit>() {

		@Override
		public void handle(WorkerInit init) {
			self = init.getPeerSelf();
			res = init.getAvailableResources();
		}
	};

	/**
	 * handling a probe reservation adding the task placeholder in the waiting
	 * queue and polling the queue to see if we have resources for any task
	 */
	Handler<Resources.Reserve> handleReserve = new Handler<Resources.Reserve>() {
		@Override
		public void handle(Resources.Reserve event) {
			res.workingQueue.waiting.add(event.getTask());

			pop();
		}
	};

	/**
	 * handling an confirmation of a task execution. The task to execute can be
	 * different from the placeholder task but it will use the same or less
	 * resources
	 */
	Handler<Resources.Allocate> handleAllocate = new Handler<Resources.Allocate>() {
		@Override
		public void handle(Resources.Allocate event) {
			if (waitingConfirmation.containsKey(event.referencId)) {

				TaskPlaceholder placeholder = waitingConfirmation
						.remove(event.referencId);
				res.release(placeholder.getNumCpus(),
						placeholder.getMemoryInMbs());

				res.allocate(event.getTask().getNumCpus(), event.getTask()
						.getMemoryInMbs());

				res.workingQueue.running.put(event.getTask().getId(),
						event.task);

				runTask(event.task);

			} else
				log.error("{} GOT ALLOCATE FOR A NOT PENDING REQUEST", selfId);
		}
	};

	/**
	 * handling a cancel for a pending requests. Means that the master already
	 * assigned the task and doesn't have any more task to give us
	 */
	Handler<Resources.Cancel> handleCancel = new Handler<Resources.Cancel>() {
		@Override
		public void handle(Resources.Cancel event) {
			if (waitingConfirmation.containsKey(event.referencId)) {

				TaskPlaceholder remove = waitingConfirmation
						.remove(event.referencId);

				res.release(remove.getNumCpus(), remove.getMemoryInMbs());

				log.debug("{} REMOVED {}", getId(), remove.getId());

				pop();

			} else
				log.error("{} GOT CANCEL FOR A NOT PENDING REQUEST", selfId);
		}
	};

	/**
	 * when a task is finished we deallocate, add it to the done queue and check
	 * if we can run more
	 */
	Handler<TaskDone> handleTaskDone = new Handler<TaskDone>() {
		@Override
		public void handle(TaskDone event) {
			Task t = (Task) res.workingQueue.running.remove(event.id);
			if (t == null)
				log.error(getId() + " " + event.id);
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

			/* last */
			pop();
		}
	};

	/**
	 * check if the next task in the waiting list can be executed. If it can, it
	 * will request a confirmation to the task master or it will allocate it
	 * directly if we are in Omniscent Oracle Mode
	 * 
	 */
	private void pop() {
		TaskPlaceholder t = (TaskPlaceholder) res.workingQueue.waiting.peek();
		if (t == null) {
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
		}
	}

	/**
	 * when requesting a confirmation to the task master we anyhow allocate
	 * temporary the resources
	 * 
	 * @param t
	 *            task to be confirmed
	 */
	private void getConfirm(TaskPlaceholder t) {
		res.allocate(t.getNumCpus(), t.getMemoryInMbs());
		waitingConfirmation.put(t.id, t);

		trigger(new Resources.Confirm(t), workerPort);

	}

	private void allocateDirectly(TaskPlaceholder placeholder) {
		// here we allocate resources and start the timers
		res.allocate(placeholder.getNumCpus(), placeholder.getMemoryInMbs());

		res.workingQueue.running.put(placeholder.getId(), placeholder);

		log.info("{} Allocated {}", getId(), placeholder.getId());

		runTask(placeholder);
	}

	/**
	 * executing a task means allocate the resources for TimeToHoldResources.
	 * 
	 * This is the only time that TimeToHoldResources is used (since in theory
	 * is not known a priori, but we need it to simulate the "execution" of the
	 * task
	 * 
	 * @param t
	 */
	private void runTask(Task t) {
		t.allocate();
		log.debug("{} RUNNING {} ({}/{}) AFTER {} QUEUE TIME ",
				new Object[] { getId(), t.getId(), res.numFreeCpus,
						res.freeMemInMbs, t.getQueueTime() });
		ScheduleTimeout tout = new ScheduleTimeout(t.getTimeToHoldResource());
		tout.setTimeoutEvent(new TaskDone(tout, t.getId()));
		trigger(tout, timerPort);
	}

}
