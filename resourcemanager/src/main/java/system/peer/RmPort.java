package system.peer;

import common.simulation.ClientRequestResource;
import resourcemanager.system.peer.rm.AllocateResources;
import se.sics.kompics.PortType;

public class RmPort extends PortType {{
	positive(ClientRequestResource.class);
//	positive(AllocateResources.class);
}}
