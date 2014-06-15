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

	private Positive<Timer> timerPort = positive(Timer.class);
	private Positive<WorkerPort> workerPort = positive(WorkerPort.class);

	private AvailableResourcesImpl res = null;
	private Address self;

	private Map<Long, TaskPlaceholder.Deferred> waitingConfirmation = new HashMap<Long, TaskPlaceholder.Deferred>();

	public RmWorker() {
		subscribe(handleInit, control);
		subscribe(handleReserve, workerPort);
		subscribe(handleAllocateDirectly, workerPort);
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

	Handler<Resources.Cancel> handleCancel = new Handler<Resources.Cancel>() {
		@Override
		public void handle(Resources.Cancel event) {
			TaskPlaceholder.Deferred placeholder = waitingConfirmation.remove(event.taskId);
			if (placeholder != null) {
				res.release(placeholder.required.numCpus, placeholder.required.memoryInMbs);
				// res.workingQueue.running.remove(remove.getId());
				log.debug(getId() + " REMOVED " + event.taskId);
				pop();
			} else {
				log.warn("Cancelling a non-waiting task?");
			}
		}
	};

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

		log.info(getId() + " Allocated {}", placeholder.getId());

		runTask(placeholder.task);
	}

	private void runTask(Task t) {
		log.debug(getId() + " RUNNING " + t.id + " (" + res.numFreeCpus
				+ "/" + res.freeMemInMbs + ")");
		t.allocate();
		ScheduleTimeout tout = new ScheduleTimeout(t.timeToHoldResource);
		tout.setTimeoutEvent(new TaskDone(tout, t.id));
		trigger(tout, timerPort);
	}

}
