package resourcemanager.system.peer.rm.task;

import common.simulation.TaskResources;

import se.sics.kompics.address.Address;

public class TaskPlaceholder {

	public abstract static class Base {
		public final Address taskMaster;
		public Base (Address taskMaster) {
			this.taskMaster = taskMaster;
		}

		public abstract int getNumCpus();
		public abstract int getMemoryInMbs();
		public abstract boolean isExecuteDirectly();
		public abstract long getId();
	}

	public static class Deferred extends Base {
		public final TaskResources required;
		public final long taskId;

		public Deferred(long taskId, Address taskMaster, TaskResources required) {
			super(taskMaster);
			this.taskId = taskId;
			this.required = required;
		}
		@Override
		public int getNumCpus() {
			return this.required.numCpus;
		}

		@Override
		public int getMemoryInMbs() {
			return this.required.memoryInMbs;
		}

		@Override
		public boolean isExecuteDirectly() {
			return false;
		}

		@Override
		public long getId() {
			return taskId;
		}
	}

	public static class Direct extends Base {
		public final Task task;

		public Direct(Address taskMaster, Task task) {
			super(taskMaster);
			this.task = task;
		}

		@Override
		public int getNumCpus() {
			return task.required.numCpus;
		}
		@Override
		public int getMemoryInMbs() {
			return task.required.memoryInMbs;
		}

		@Override
		public boolean isExecuteDirectly() {
			return true;
		}
		
		public long getId() {
			return this.task.id;
		}
	}

}