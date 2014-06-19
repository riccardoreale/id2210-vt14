package resourcemanager.system.peer.rm.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import common.simulation.TaskResources;

import fdet.system.evts.FdetPort;

import resourcemanager.system.peer.rm.Resources;
import resourcemanager.system.peer.rm.task.TaskPlaceholder.Deferred;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;

public class RmWorker extends ComponentDefinition {

	public class ObjectId {

		private String cached = null;

		public String toString() {
			if (cached == null) {
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
				stringBuilder.append(self.getIp().getHostAddress());
				stringBuilder.append("]");

				cached = stringBuilder.toString();
			}
			return cached;
		}

	}

	private class Borrowers {
		private Map<Address, Integer> map = new HashMap<Address, Integer>();

		public void lend(Address to, TaskResources what) {
			Integer n = map.get(to);
			map.put(to, n == null ? 1 : n + 1);
			res.allocate(what);
			if(!to.equals(self))
				trigger(new FdetPort.Subscribe(to), fdetPort);
		}

		public void claim(Address to, TaskResources what) {
			Integer n = map.get(to);
			assert n != null;
			if (n == 1) {
				map.remove(to);
				if(!to.equals(self))
					trigger(new FdetPort.Unsubscribe(to), fdetPort);
			} else {
				assert n > 1;
				map.put(to, n - 1);
			}
			res.release(what);
		}
		
		public int countCredits(Address of) {
			Integer out = map.get(of);
			return out == null ? 0 : out;
		}

		private void updateCredit(Address to, TaskResources prev, TaskResources next) {
			assert map.containsKey(to);
			res.release(prev);
			res.allocate(next);
		}
	};

	private static final Logger log = LoggerFactory.getLogger(RmWorker.class);

	private Positive<Timer> timerPort = positive(Timer.class);
	private Positive<WorkerPort> workerPort = positive(WorkerPort.class);
	private Positive<FdetPort> fdetPort = positive(FdetPort.class);

	private AvailableResourcesImpl res = null;
	private Address self;
	private ObjectId selfId = new ObjectId();
	private Borrowers borrowers = new Borrowers();

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
			ArrayList<Long> removed = new ArrayList<Long>();
			boolean popAfter = false;
			
			Iterator<Entry<Long, TaskPlaceholder.Direct>> i = res.workingQueue.running.entrySet().iterator();
			while (i.hasNext()) {
				Entry<Long, TaskPlaceholder.Direct> e = i.next();
				TaskPlaceholder.Direct tph = e.getValue();
				if (tph.taskMaster.equals(event.ref)){
					borrowers.claim(event.ref, tph.task.required);
					removed.add(e.getKey());
					i.remove();
					popAfter = true;
				}
			}

			Iterator<Entry<Long, Deferred>> j = waitingConfirmation.entrySet().iterator();
			while (j.hasNext()) {
				Entry<Long, TaskPlaceholder.Deferred> e = j.next();
				TaskPlaceholder.Deferred tph = e.getValue();
				if (tph.taskMaster.equals(event.ref)){
					borrowers.claim(event.ref, tph.required);
					removed.add(e.getKey());
					j.remove();
				}
			}

			assert borrowers.countCredits(event.ref) == 0;
			log.debug(getId() + ": {} DETECTED AS DEAD. RELEASED RESOURCES FOR {}", event.ref.getIp().getHostAddress(), removed);

			/* If we eliminated at least one running task, the pop() call will be skipped.
			 * We need therefore to do it here. */
			if (popAfter) pop();
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
			log.info("{} Allocating task {}", getId(), event.task.id);
			borrowers.updateCredit(placeholder.taskMaster, placeholder.required, event.task.required);
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
				borrowers.claim(placeholder.taskMaster, placeholder.required);
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
			if (tph == null) {
				log.error(getId() + " IGNORING TASK " + event.referenceId + ", TASK MASTER WAS FAULTY");
			} else {
				Task t = tph.task;
				t.deallocate();
				borrowers.claim(tph.taskMaster, t.required);
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
		borrowers.lend(t.taskMaster, t.required);
		waitingConfirmation.put(t.getId(), t);

		trigger(new Resources.Confirm(t), workerPort);
	}

	private void allocateDirectly(TaskPlaceholder.Direct placeholder) {
		// here we allocate resources and start the timers
		borrowers.lend(placeholder.taskMaster, placeholder.task.required);

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
