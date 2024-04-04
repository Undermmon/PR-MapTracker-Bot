/*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at https://mozilla.org/MPL/2.0/.
*/

package me.undermon.maplogger.configuration;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.MalformedJsonException;

public final class Configuration implements Iterable<MonitoredServer>{
	private static final Gson GSON = new Gson();
	
	private String token;
	private ZoneId timezone;
	private Duration fetchInterval;
	private Map<String, String> monitored_servers;
	private URI realityAPI;
	private MonitoredServer defaultMonitoredServer;

	private record ConfigFromJson(
		String realitymod_api, String token, String timezone,  int fetch_interval, HashMap<String, String> monitored_servers
	) {}

	private Configuration() throws InvalidConfigurationException {
		try {
			var desserialized = GSON.fromJson(Files.readString(Paths.get("config.json")), ConfigFromJson.class);
			
			this.token = Objects.requireNonNull(desserialized.token, "you must provide a Discord bot token");

			this.realityAPI = new URI(Objects.requireNonNull(desserialized.realitymod_api));

			if (desserialized.fetch_interval < 1) {
				throw new IllegalArgumentException("fetch interval must be one or greater");
			}

			this.timezone = ZoneId.of(desserialized.timezone);

			this.fetchInterval = Duration.ofMinutes(desserialized.fetch_interval);
			
			if (desserialized.monitored_servers.size() < 1) {
				throw new IllegalArgumentException("there must be at least one monitored server");
			}

			this.monitored_servers = desserialized.monitored_servers;

			this.defaultMonitoredServer = this.stream().findFirst().get();

		} catch (JsonSyntaxException e) {
			if (e.getCause() instanceof NumberFormatException) {
				throw new InvalidConfigurationException("one or more values can't be converted to a number");
			}

			if (e.getCause() instanceof MalformedJsonException) {
				throw new InvalidConfigurationException("file is syntax is not valid");
			}

		} catch (URISyntaxException e) {
			throw new InvalidConfigurationException("link to RealityMod's api is not a valid url");

		} catch (IllegalArgumentException e) {
			throw new InvalidConfigurationException(e.getMessage());

		} catch (DateTimeException e) {
			throw new InvalidConfigurationException(e.getMessage());

		} catch (Exception e) {
			throw new InvalidConfigurationException(e.getMessage());
		}
	}

	public String token() {
		return token;
	}
	
	public URI realitymodAPI() {
		return this.realityAPI;
	}

	public ZoneId getTimezone() {
		return timezone;
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

	public MonitoredServer getDefaultMonitoredServer() {
		return this.defaultMonitoredServer;
	}

	public static Configuration readFromDisk() throws InvalidConfigurationException {
		return new Configuration();
	}
}
