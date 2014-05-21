package resourcemanager.system.peer.rm.task;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;


public class WorkingQueue {

	Map<Long, Task> running = new HashMap<Long, Task>();
	Queue<Task> waiting = new LinkedList<Task>();
	List<Task> done = new LinkedList<Task>();

}
