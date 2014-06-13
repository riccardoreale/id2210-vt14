package resourcemanager.system.peer.rm.task;

import resourcemanager.system.peer.rm.Resources;
import se.sics.kompics.PortType;

public class WorkerPort extends PortType {
	{
		positive(Resources.Allocate.class);
		positive(Resources.Reserve.class);
		positive(Resources.Cancel.class);
		negative(Resources.Confirm.class);
		negative(Resources.Completed.class);
	}
}
