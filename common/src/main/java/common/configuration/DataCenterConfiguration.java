package common.configuration;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

public class DataCenterConfiguration {

	public final boolean omniscent;
	public final float load;

	public DataCenterConfiguration(boolean omniscent, float load) {
		this.omniscent = omniscent;
		this.load = load;
	}

	public void store(String file) throws IOException {
		Properties p = new Properties();
		p.setProperty("omniscent", Boolean.toString(this.omniscent));
		p.setProperty("load", Float.toString(this.load));

		Writer writer = new FileWriter(file);
		p.store(writer, "se.sics.kompics.p2p.overlay.application");
	}

	public static DataCenterConfiguration load(String file) throws IOException {
		Properties p = new Properties();
		Reader reader = new FileReader(file);
		p.load(reader);

		boolean omniscent = Boolean.parseBoolean(p.getProperty("omniscent"));
		float load = Float.parseFloat(p.getProperty("load"));

		return new DataCenterConfiguration(omniscent, load);
	}
}
