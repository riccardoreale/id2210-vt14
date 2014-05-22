/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package resourcemanager.system.peer.rm.task;

import common.peer.AvailableResources;

/**
 * 
 * @author jdowling
 */
public class AvailableResourcesImpl implements AvailableResources {

	volatile int numFreeCpus;
	volatile int freeMemInMbs;

	WorkingQueue workingQueue;

	public AvailableResourcesImpl(int numFreeCpus, int freeMemInMbs) {
		this.numFreeCpus = numFreeCpus;
		this.freeMemInMbs = freeMemInMbs;
		this.workingQueue = new WorkingQueue();
	}

	public synchronized boolean isAvailable(int numCpus, int memInMbs) {
		if (numFreeCpus >= numCpus && freeMemInMbs >= memInMbs) {
			return true;
		}
		return false;
	}

	public synchronized boolean allocate(int numCpus, int memInMbs) {
		if (numFreeCpus >= numCpus && freeMemInMbs >= memInMbs) {
			numFreeCpus -= numCpus;
			freeMemInMbs -= memInMbs;
			return true;
		}
		return false;
	}

	public synchronized void release(int numCpus, int memInMbs) {
		if (numCpus <= 0 || memInMbs <= 0) {
			throw new IllegalArgumentException("Invalid numbCpus or mem");
		}
		numFreeCpus += numCpus;
		freeMemInMbs += memInMbs;
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
}
