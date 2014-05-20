package system.peer;

import common.simulation.RequestResource;
import resourcemanager.system.peer.rm.AllocateResources;
import se.sics.kompics.PortType;

public class RmPort extends PortType {{
	positive(RequestResource.class);
	positive(AllocateResources.class);
}}
