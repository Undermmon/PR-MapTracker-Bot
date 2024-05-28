/*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at https://mozilla.org/MPL/2.0/.
*/

package me.undermon.maplogger.configuration;

import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class Configuration implements Iterable<TrackedServer>{

	private String token;
	private Duration fetchInterval;
	private List<TrackedServer> servers = new ArrayList<>();
	private URI realityAPI;

	public static Configuration readFromDisk() {
		return new Configuration();
	}

	private Configuration() {
		try (FileReader fileReader = new FileReader("maptracker.properties")) {
			Properties properties = new Properties();
			properties.load(fileReader);

			this.token = properties.getProperty("token");

			if (this.token.isBlank()) {
				throw new InvalidConfigurationException();
			}

			this.fetchInterval = Duration.ofMinutes(Integer.parseInt(properties.getProperty("fetchInterval").strip()));
			this.realityAPI = new URI(properties.getProperty("serverInfoAPI"));

			var serverNames = properties.getProperty("serverNames").split(" ");
			var serverIds = properties.getProperty("serverIds").split(" ");

			if (serverIds.length != serverNames.length) {
				throw new InvalidConfigurationException();
			}

			for (int i = 0; i < serverIds.length; i++) {
				this.servers.add(new TrackedServer(serverNames[i].replace("_", " "), serverIds[i]));
			}

		} catch (IOException | URISyntaxException | InvalidConfigurationException e) {
			e.printStackTrace();
		} 
	}

	public String token() {
		return token;
	}
	
	public URI serverInfoAPI() {
		return this.realityAPI;
	}

	public ZoneId getTimezone() {
		return ZoneId.systemDefault();
	}

	public Duration fetchInterval() {
		return this.fetchInterval;
	}

	public TrackedServer primaryTrackedServer() {
		return this.servers.get(0);
	}

	public Stream<TrackedServer> stream() {
		return StreamSupport.stream(this.spliterator(), false);
	}

	@Override
	public Iterator<TrackedServer> iterator() {
		return servers.iterator();
	}

}
