/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package resourcemanager.system.peer.rm.task;

import common.peer.AvailableResources;
import common.simulation.TaskResources;

/**
 * 
 * @author jdowling
 */
public class AvailableResourcesImpl implements AvailableResources {

	volatile int numFreeCpus;
	volatile int freeMemInMbs;
	private int totalCpus;
	private int totalMemory;

	WorkingQueue workingQueue;

	public String toString() {
		return String.format("CPUs: %d/%d free, Memory: %d/%d free", numFreeCpus,
			totalCpus, freeMemInMbs, totalMemory
		);
	}

	public AvailableResourcesImpl(int numFreeCpus, int freeMemInMbs) {
		this.numFreeCpus = numFreeCpus;
		this.freeMemInMbs = freeMemInMbs;
		this.workingQueue = new WorkingQueue();
		this.totalCpus = numFreeCpus;
		this.totalMemory = freeMemInMbs;
	}

	public synchronized boolean isAvailable(int numCpus, int memInMbs) {
		if (numFreeCpus >= numCpus && freeMemInMbs >= memInMbs) {
			return true;
		}
		return false;
	}

	public synchronized boolean allocate(int numCpus, int memInMbs) {
		if (numCpus <= 0 || memInMbs <= 0) {
			throw new IllegalArgumentException("Invalid numbCpus or mem");
		}
		if (numFreeCpus >= numCpus && freeMemInMbs >= memInMbs) {
			numFreeCpus -= numCpus;
			freeMemInMbs -= memInMbs;
			return true;
		}
		if (numFreeCpus < 0 || freeMemInMbs < 0) {
			throw new IllegalArgumentException("Allocating too much");
		}
		return false;
	}

	public synchronized void release(int numCpus, int memInMbs) {
		if (numCpus <= 0 || memInMbs <= 0) {
			throw new IllegalArgumentException("Invalid numbCpus or mem");
		}
		numFreeCpus += numCpus;
		freeMemInMbs += memInMbs;
		if (numFreeCpus > totalCpus || freeMemInMbs > totalMemory) {
			throw new IllegalArgumentException("Releasing too much");
		}
	}

	public int getNumFreeCpus() {
		return numFreeCpus;
	}

	public int getFreeMemInMbs() {
		return freeMemInMbs;
	}

	public int getWorkingQueueTime(int numCpus, int memInMbs) {
		if (isAvailable(numCpus, memInMbs))
			return 0;
		else
			return workingQueue.getWorkingQueueTime(numFreeCpus, freeMemInMbs,
					numCpus, memInMbs);
	}

	public WorkingQueue getWorkingQueue() {
		return workingQueue;
	}

	@Override
	public int getTotalCpus() {
		return totalCpus;
	}

	@Override
	public int getTotalMemory() {
		return totalMemory;
	}

	@Override
	public int getQueueLength() {
		return workingQueue.getWaiting().size();
	}

	public void allocate(TaskResources r) {
		allocate(r.numCpus, r.memoryInMbs);
	}

	public void release(TaskResources r) {
		release(r.numCpus, r.memoryInMbs);
	}
}
