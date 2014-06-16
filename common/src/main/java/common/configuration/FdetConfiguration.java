package common.configuration;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

public class FdetConfiguration {
	
	public final long timeout;
	
	public FdetConfiguration(long timeout) {
		this.timeout = timeout;
	}

	public void store(String file) throws IOException {
		Properties p = new Properties();
		p.setProperty("timeout", Long.toString(this.timeout));

		Writer writer = new FileWriter(file);
		p.store(writer, "se.sics.kompics.p2p.overlay.application");
	}

	public static FdetConfiguration load(String file) throws IOException {
		Properties p = new Properties();
		Reader reader = new FileReader(file);
		p.load(reader);

		long timeout = Long.parseLong(p.getProperty("timeout"));

		return new FdetConfiguration(timeout);
	}

}
