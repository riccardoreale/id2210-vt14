package resourcemanager.system.peer.rm.task;

import resourcemanager.system.peer.rm.AllocateResources;
import se.sics.kompics.PortType;

public class WorkerPort extends PortType {{
	positive(AllocateResources.class);
}}
