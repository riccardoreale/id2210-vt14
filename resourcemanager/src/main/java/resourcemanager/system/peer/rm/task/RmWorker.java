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
				log.warn("AAAA");
		}
	};

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
				log.warn("CCCC");
		}
	};

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

		log.info("{} Allocated {}", getId(), placeholder.getId());

		runTask(placeholder);
	}

	private void runTask(Task t) {
		log.debug("{} RUNNING {} ({}/{})", new Object[] { getId(), t.getId(),
				res.numFreeCpus, res.freeMemInMbs });
		t.allocate();
		ScheduleTimeout tout = new ScheduleTimeout(t.getTimeToHoldResource());
		tout.setTimeoutEvent(new TaskDone(tout, t.getId()));
		trigger(tout, timerPort);
	}

}
