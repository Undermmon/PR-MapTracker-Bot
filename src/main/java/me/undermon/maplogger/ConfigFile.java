package me.undermon.maplogger;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.gson.Gson;

public class ConfigFile implements Iterable<ConfigFile.MonitoredServer>{
	private static final Gson GSON = new Gson();
	
	private String token;
	private Duration fetchInterval;
	private Map<String, String> monitored_servers;
	private URI realityAPI;

	public record MonitoredServer(String label, String identifier) {}
	
	private record ConfigJson(
		String realitymod_api, String token, int fetch_interval, HashMap<String, String> monitored_servers
	) {}

	public final class InvalidConfigurationFile extends Exception {}

	private ConfigFile() throws InvalidConfigurationFile {
		try {
			var desserialized = GSON.fromJson(Files.readString(Paths.get("config.json")), ConfigJson.class);
			
			this.token = Objects.requireNonNull(desserialized.token);

			this.realityAPI = new URI(Objects.requireNonNull(desserialized.realitymod_api));

			if (desserialized.fetch_interval < 1) {
				throw new IllegalArgumentException();
			}
			this.fetchInterval = Duration.ofMinutes(desserialized.fetch_interval);
			
			this.monitored_servers = desserialized.monitored_servers;

		} catch (Exception e) {
			throw new InvalidConfigurationFile();
		}
	}

	public String token() {
		return token;
	}
	
	public URI realitymodAPI() {
		return this.realityAPI;
	}
	
	public Duration fetchInterval() {
		return this.fetchInterval;
	}

	@Override
	public Iterator<MonitoredServer> iterator() {
		return new MonitoredServerIterator();
	}

	private class MonitoredServerIterator implements Iterator<MonitoredServer>{
		Iterator<Entry<String, String>> monitored = monitored_servers.entrySet().iterator();

		@Override
		public boolean hasNext() {
			return monitored.hasNext();
		}

		@Override
		public MonitoredServer next() {
			var next = monitored.next();

			return new MonitoredServer(next.getKey(), next.getValue());
		}
	}

	public Stream<MonitoredServer> stream() {
		return StreamSupport.stream(this.spliterator(), false);
	}

	public static ConfigFile read() throws InvalidConfigurationFile {
		return new ConfigFile();
	}
}
