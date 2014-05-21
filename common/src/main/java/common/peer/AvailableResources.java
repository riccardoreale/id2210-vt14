/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package common.peer;

/**
 *
 * @author jdowling
 */
public class AvailableResources {

    private volatile int numFreeCpus;
    private volatile int freeMemInMbs;
    
    /* Hacky way of knowing the queue length. This is used for the omniscent
    * scheduler. Just pretend it's not here... */
    private volatile int queueLength;

    public AvailableResources(int numFreeCpus, int freeMemInMbs) {
        this.numFreeCpus = numFreeCpus;
        this.freeMemInMbs = freeMemInMbs;
        this.queueLength = 0;
    }
    
    public synchronized void setQueueLength (int queueLenght) {
    	this.queueLength = queueLenght;
    }

    public int getQueueLength() {
		return queueLength;
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
}
