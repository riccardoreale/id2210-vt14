package common.configuration;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import se.sics.kompics.address.Address;
import se.sics.kompics.p2p.bootstrap.BootstrapConfiguration;

public class Configuration {

	private static final int GOSSIP_PERIOD = 250;
	public static int SNAPSHOT_PERIOD = 1000;
	public static int AVAILABLE_TOPICS = 20;
	public InetAddress ip = null;

	{
		try {
			ip = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			ip = Inet4Address.getByAddress(new byte[] { 127, 0, 0, 1 });
		}
	}
	int webPort = 8080;
	int bootId = Integer.MAX_VALUE;
	int networkPort = 8081;
	Address bootServerAddress = new Address(ip, networkPort, bootId);
	final long seed;
	BootstrapConfiguration bootConfiguration = new BootstrapConfiguration(
			bootServerAddress, 60000, 4000, 3, 30000, webPort, webPort);
	CyclonConfiguration cyclonConfiguration;
	TManConfiguration tmanConfiguration;
	RmConfiguration searchConfiguration;
	DataCenterConfiguration dataCenterConfiguration;
	FdetConfiguration fdetConfiguration;

	public Configuration(long seed, boolean omniscent, int probes, float load, long fdetTimeout)
			throws IOException {
		this.seed = seed;
		dataCenterConfiguration = new DataCenterConfiguration(omniscent, load);
		searchConfiguration = new RmConfiguration(seed, omniscent, probes);
		tmanConfiguration = new TManConfiguration(seed, GOSSIP_PERIOD, 0.8);
		cyclonConfiguration = new CyclonConfiguration(seed, 5, 10,
				GOSSIP_PERIOD, 500000,
				(long) (Integer.MAX_VALUE - Integer.MIN_VALUE), 20);
		fdetConfiguration = new FdetConfiguration(fdetTimeout);

		String c = File.createTempFile("bootstrap.", ".conf").getAbsolutePath();
		bootConfiguration.store(c);
		System.setProperty("bootstrap.configuration", c);

		c = File.createTempFile("cyclon.", ".conf").getAbsolutePath();
		cyclonConfiguration.store(c);
		System.setProperty("cyclon.configuration", c);

		c = File.createTempFile("tman.", ".conf").getAbsolutePath();
		tmanConfiguration.store(c);
		System.setProperty("tman.configuration", c);

		c = File.createTempFile("rm.", ".conf").getAbsolutePath();
		searchConfiguration.store(c);
		System.setProperty("rm.configuration", c);

		c = File.createTempFile("datacenter.", ".conf").getAbsolutePath();
		dataCenterConfiguration.store(c);
		System.setProperty("datacenter.configuration", c);

		c = File.createTempFile("fdet.", ".conf").getAbsolutePath();
		fdetConfiguration.store(c);
		System.setProperty("fdet.configuration", c);
	}
}
