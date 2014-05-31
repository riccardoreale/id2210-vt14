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
public interface AvailableResources {

	public int getNumFreeCpus();

	public int getFreeMemInMbs();

	public int getTotalCpus();

	public int getTotalMemory();

	public int getWorkingQueueTime(int numCpus, int memInMbs);

}
