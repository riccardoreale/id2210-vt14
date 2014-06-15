package resourcemanager.system.peer.rm.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class WorkingQueue {

	public final Map<Long, TaskPlaceholder.Direct> running = new HashMap<Long, TaskPlaceholder.Direct>();
	public final Queue<TaskPlaceholder.Base> waiting = new LinkedList<TaskPlaceholder.Base>();
	public final List<Task> done = new LinkedList<Task>();

	public int getWorkingQueueTime(int tmpCpu, int tmpMem, int numCpus,
			int memInMbs) {

		// rather complex algorithm, but works

		// we maintain timers and virtualCpu and Mem
		int workingTime = 0;
		final long now = System.currentTimeMillis();

		// lists of running and waiting
		Iterator<TaskPlaceholder.Base> waitingIterator = waiting.iterator();
		ArrayList<TaskPlaceholder.Direct> listRunning = new ArrayList<TaskPlaceholder.Direct>(running.values());
		TaskPlaceholder.Direct nextPlaceHolder = null;

		// first we sort running tasks by the fastest to finish
		Collections.sort(listRunning, new Comparator<TaskPlaceholder.Direct>() {
			@Override
			public int compare(TaskPlaceholder.Direct o1, TaskPlaceholder.Direct o2) {
				Task t1 = o1.task;
				Task t2 = o2.task;
				return (int) ((t1.timeToHoldResource - (now - t1.allocateTime)) - (t2.timeToHoldResource - (now - t2.allocateTime)));
			}
		});

		// we virtually "execute" one task at the time
		for (int i = 0; i < listRunning.size(); i++) {

			// executing a task means increase the virtual time
			// and release resources
			Task t = listRunning.get(i).task;
			workingTime += t.timeToHoldResource - (now - t.allocateTime);
			tmpCpu += t.required.numCpus;
			tmpMem += t.required.memoryInMbs;

			// once resources are released we check if there is a waiting
			// task
			if (nextPlaceHolder == null) {
				if (waitingIterator.hasNext()) {
					// This cast should never fail, as we run it in omniscent mode
					TaskPlaceholder.Direct tmp = (TaskPlaceholder.Direct) waitingIterator.next();
					nextPlaceHolder = new TaskPlaceholder.Direct(tmp.taskMaster, tmp.task.copy());
				}
			}

			// if there is a waiting task, we check if we can allocate it
			// immediately, otherwise we wait and "execute" another running
			// task
			// if there are no other tasks, we "execute" running tasks until
			// the requested task can be allocated
			if (nextPlaceHolder != null) {
				Task nextTask = nextPlaceHolder.task;
				if (tmpCpu >= nextTask.required.numCpus
						&& tmpMem >= nextTask.required.memoryInMbs) {
					listRunning.add(nextPlaceHolder);
					nextTask.allocateTime = now;
					nextPlaceHolder = null;
				}
			} else if (tmpCpu >= numCpus && tmpMem >= memInMbs)
				return workingTime;
		}

		// if we "executed" all tasks and still we can't allocate a new one
		// means that we don't have enough resources to run it (shouldn't
		// happened). anyhow we return "infinite" time
		return Integer.MAX_VALUE;

	}

}
