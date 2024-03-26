package me.undermon.maplogger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class ConfigFile implements Iterable<ConfigFile.MonitoredServer>{
	private static final Gson GSON = new Gson();
	
	private String token;
	private Duration fetchInterval;
	private Map<String, String> monitored_servers;

	private URI realityAPI;

	public record MonitoredServer(String label, String identifier) {}
	
	private record ConfigJson(
		String realitymod_api, String token, int fetch_interval, HashMap<String, String> monitored_servers) {}

	private ConfigFile() throws URISyntaxException, JsonSyntaxException, IOException {
		var desserialized = GSON.fromJson(Files.readString(Paths.get("config.json")), ConfigJson.class);
		
		this.token = desserialized.token;
		this.realityAPI = new URI(desserialized.realitymod_api);
		this.fetchInterval = Duration.ofMinutes(desserialized.fetch_interval);
		this.monitored_servers = desserialized.monitored_servers;
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

	public static ConfigFile read() throws JsonSyntaxException, URISyntaxException, IOException {
		return new ConfigFile();
	}
}
