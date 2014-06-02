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

	Map<Long, Task> running = new HashMap<Long, Task>();
	Queue<TaskPlaceholder> waiting = new LinkedList<TaskPlaceholder>();
	private List<Task> done = new LinkedList<Task>();

	public int getWorkingQueueTime(int tmpCpu, int tmpMem, int numCpus,
			int memInMbs) {

		// rather complex algorithm, but works

		// we maintain timers and virtualCpu and Mem
		int workingTime = 0;
		final long now = System.currentTimeMillis();

		// lists of running and waiting
		Iterator<TaskPlaceholder> waitingIterator = waiting.iterator();
		ArrayList<Task> listRunning = new ArrayList<Task>(running.values());
		Task nextTask = null;

		// first we sort running tasks by the fastest to finish
		Collections.sort(listRunning, new Comparator<Task>() {
			@Override
			public int compare(Task o1, Task o2) {
				return (int) ((o1.timeToHoldResource - (now - o1.allocateTime)) - (o2.timeToHoldResource - (now - o2.allocateTime)));
			}
		});

		// we virtually "execute" one task at the time
		for (int i = 0; i < listRunning.size(); i++) {

			// executing a task means increase the virtual time
			// and release resources
			Task t = listRunning.get(i);
			workingTime += t.timeToHoldResource - (now - t.allocateTime);
			tmpCpu += t.numCpus;
			tmpMem += t.memoryInMbs;

			// once resources are released we check if there is a waiting
			// task
			if (nextTask == null) {
				if (waitingIterator.hasNext()) {
					TaskPlaceholder tmp = waitingIterator.next();
					nextTask = new Task(tmp.id, tmp.numCpus, tmp.memoryInMbs,
							tmp.timeToHoldResource);
				}
			}

			// if there is a waiting task, we check if we can allocate it
			// immediately, otherwise we wait and "execute" another running
			// task
			// if there are no other tasks, we "execute" running tasks until
			// the requested task can be allocated
			if (nextTask != null) {
				if (tmpCpu >= nextTask.numCpus
						&& tmpMem >= nextTask.memoryInMbs) {
					listRunning.add(nextTask);
					nextTask.allocateTime = now;
					nextTask = null;
				}
			} else if (tmpCpu >= numCpus && tmpMem >= memInMbs)
				return workingTime;
		}

		// if we "executed" all tasks and still we can't allocate a new one
		// means that we don't have enough resources to run it (shouldn't
		// happened). anyhow we return "infinite" time
		return Integer.MAX_VALUE;

	}

	public List<Task> getDone() {
		return done;
	}

}
