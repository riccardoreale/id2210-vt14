package resourcemanager.system.peer.rm.task;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fdet.system.evts.FdetPort;

import resourcemanager.system.peer.rm.Resources;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
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

	private Positive<Timer> timerPort = positive(Timer.class);
	private Positive<WorkerPort> workerPort = positive(WorkerPort.class);
	private Positive<FdetPort> fdetPort = positive(FdetPort.class);

	private AvailableResourcesImpl res = null;
	private Address self;
	private ObjectId selfId = new ObjectId();

	private Map<Long, TaskPlaceholder.Deferred> waitingConfirmation = new HashMap<Long, TaskPlaceholder.Deferred>();

	public RmWorker() {
		subscribe(handleInit, control);
		subscribe(handleReserve, workerPort);
		subscribe(handleAllocateDirectly, workerPort);
		subscribe(handleAllocate, workerPort);
		subscribe(handleCancel, workerPort);
		subscribe(handleTaskDone, timerPort);
		subscribe(handleNodeFailure, fdetPort);
		subscribe(handleNodeRestore, fdetPort);
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
			TaskPlaceholder.Deferred tph = new TaskPlaceholder.Deferred(event.taskId,
				event.taskMaster, event.required);
			res.workingQueue.waiting.add(tph);
			pop();
		}
	};

	Handler<Resources.AllocateDirectly> handleAllocateDirectly = new Handler<Resources.AllocateDirectly>() {
		@Override
		public void handle(Resources.AllocateDirectly event) {
			TaskPlaceholder.Direct tph = new TaskPlaceholder.Direct(event.taskMaster, event.task);
			res.workingQueue.waiting.add(tph);
			pop();
		}
	};

	Handler<FdetPort.Dead> handleNodeFailure = new Handler<FdetPort.Dead>() {
		@Override
		public void handle(FdetPort.Dead event) {
			// TODO Auto-generated method stub
		}
	};

	Handler<FdetPort.Undead> handleNodeRestore = new Handler<FdetPort.Undead>() {
		@Override
		public void handle(FdetPort.Undead event) {
			// TODO Auto-generated method stub
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
			TaskPlaceholder.Deferred placeholder = waitingConfirmation.remove(event.originalTaskId);
			assert placeholder != null;
			log.info("Allocating waiting task");
			res.release(placeholder.getNumCpus(), placeholder.getMemoryInMbs());
			res.allocate(event.task.required.numCpus, event.task.required.memoryInMbs);
			TaskPlaceholder.Direct run = new TaskPlaceholder.Direct(event.taskMaster, event.task);
			res.workingQueue.running.put(event.task.id, run);
			runTask(event.task);
		}
	};

	/**
	 * handling a cancel for a pending requests. Means that the master already
	 * assigned the task and doesn't have any more task to give us
	 */
	Handler<Resources.Cancel> handleCancel = new Handler<Resources.Cancel>() {
		@Override
		public void handle(Resources.Cancel event) {
			TaskPlaceholder.Deferred placeholder = waitingConfirmation.remove(event.taskId);
			if (placeholder != null) {
				res.release(placeholder.required.numCpus, placeholder.required.memoryInMbs);
				// res.workingQueue.running.remove(remove.getId());
				log.debug("{} REMOVED {}", getId(), event.taskId);
				pop();
			} else {
				log.warn("Cancelling a non-waiting task?");
			}
		}
	};

	/**
	 * when a task is finished we deallocate, add it to the done queue and check
	 * if we can run more
	 */
	Handler<TaskDone> handleTaskDone = new Handler<TaskDone>() {
		@Override
		public void handle(TaskDone event) {
			TaskPlaceholder.Direct tph = res.workingQueue.running.remove(event.referenceId);
			if (tph == null)
				log.error(getId() + " " + event.referenceId);
			assert tph != null;
			Task t = tph.task;
			t.deallocate();
			res.release(t.required.numCpus, t.required.memoryInMbs);
			res.workingQueue.done.add(t);
			log.info(
					"{} Done {}, QueueTime={}, TotalTime={}",
					new Object[] {
							getId(),
							t.id,
							t.getQueueTime(),
							(float) (t.timeToHoldResource)
									/ t.getTotalTime() });
			trigger(new Resources.Completed(tph.taskMaster, t), workerPort);

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
		TaskPlaceholder.Base t = res.workingQueue.waiting.peek();
		if (t == null) {
			// FIXME: probably this could be a single assertion: !(a && b) || c
			if (res.workingQueue.running.size() == 0)
				if (waitingConfirmation.size() == 0)
					assert res.getNumFreeCpus() == res.getTotalCpus();
			return;
		}
		if (res.isAvailable(t.getNumCpus(), t.getMemoryInMbs())) {
			res.workingQueue.waiting.poll();

			if (t.isExecuteDirectly()) {
				allocateDirectly((TaskPlaceholder.Direct)t);
			} else {
				getConfirm((TaskPlaceholder.Deferred)t);
			}
		}
	}

	/**
	 * when requesting a confirmation to the task master we anyhow allocate
	 * temporary the resources
	 * 
	 * @param t
	 *            task to be confirmed
	 */
	private void getConfirm(TaskPlaceholder.Deferred t) {
		// here we temporary block resources
		// and ask for confirmation
		res.allocate(t.getNumCpus(), t.getMemoryInMbs());
		waitingConfirmation.put(t.getId(), t);

		trigger(new Resources.Confirm(t), workerPort);
	}

	private void allocateDirectly(TaskPlaceholder.Direct placeholder) {
		// here we allocate resources and start the timers
		res.allocate(placeholder.getNumCpus(), placeholder.getMemoryInMbs());

		res.workingQueue.running.put(placeholder.getId(), placeholder);

		log.info("{} Allocated {}", getId(), placeholder.getId());

		runTask(placeholder.task);
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
				new Object[] { getId(), t.id, res.numFreeCpus,
				res.freeMemInMbs, t.getQueueTime() });
		ScheduleTimeout tout = new ScheduleTimeout(t.timeToHoldResource);
		tout.setTimeoutEvent(new TaskDone(tout, t.id));
		trigger(tout, timerPort);
	}

}
