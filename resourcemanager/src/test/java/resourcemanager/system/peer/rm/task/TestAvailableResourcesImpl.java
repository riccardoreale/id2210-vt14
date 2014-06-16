package resourcemanager.system.peer.rm.task;

import junit.framework.Assert;

import org.junit.Test;

import common.simulation.TaskResources;

public class TestAvailableResourcesImpl {

	@Test
	public void testEmptyQueue() {
		AvailableResourcesImpl res = new AvailableResourcesImpl(8, 12000);
		Assert.assertEquals(0, res.getWorkingQueueTime(2, 2));
	}

	@Test
	public void testInfiniteQueue() {
		AvailableResourcesImpl res = new AvailableResourcesImpl(8, 12000);
		Assert.assertEquals(Integer.MAX_VALUE, res.getWorkingQueueTime(10, 2));
	}

	@Test
	public void testFullQueueNoWaiting() {
		final long now = System.currentTimeMillis();
		AvailableResourcesImpl res = new AvailableResourcesImpl(8, 12000);
		for (long i = 0; i < 4; i++) {

			res.allocate(2, 2);
			Task task = new Task(i, new TaskResources(2, 2), 60000);
			task.allocateTime = now - (4 - i) * 10000;
			res.workingQueue.running.put(i, new TaskPlaceholder.Direct(null, task));
		}

		int workingQueueTime = res.getWorkingQueueTime(2, 2);
		Assert.assertTrue(workingQueueTime <= 20000);
		Assert.assertTrue(workingQueueTime > 19000);
	}

	@Test
	public void testFullQueueWithWaiting() {
		final long now = System.currentTimeMillis();
		AvailableResourcesImpl res = new AvailableResourcesImpl(8, 12000);
		for (long i = 0; i < 4; i++) {

			res.allocate(2, 2);
			Task task = new Task(i, new TaskResources(2, 2), 60000);
			task.allocateTime = now;
			res.workingQueue.running.put(i, new TaskPlaceholder.Direct(null, task));
		}

		for (long i = 0; i < 4; i++) {

			Task task = new Task(i, new TaskResources(4, 2), 60000);
			res.workingQueue.waiting.add(new TaskPlaceholder.Direct(null, task));
		}

		int workingQueueTime = res.getWorkingQueueTime(2, 2);
		Assert.assertTrue(workingQueueTime <= 360000);
		Assert.assertTrue(workingQueueTime > 359000);
	}
}
