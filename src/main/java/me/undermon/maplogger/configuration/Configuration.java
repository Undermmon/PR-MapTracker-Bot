/*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at https://mozilla.org/MPL/2.0/.
*/

package me.undermon.maplogger.configuration;

import java.io.FileNotFoundException;
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

	private static final String FILE_NAME = "maptracker.properties";

	private String token;
	private Duration fetchInterval;
	private List<TrackedServer> servers = new ArrayList<>();
	private URI realityAPI;

	public static Configuration readFromDisk() {
		return new Configuration();
	}

	private Configuration() {
		try (FileReader fileReader = new FileReader(FILE_NAME)) {
			Properties properties = new Properties();
			properties.load(fileReader);

			this.token = properties.getProperty("token").strip();

			if (this.token.isBlank()) {
				throw new ConfigurationFileException("Token must not be blank.");
			}

			if (this.token.contains(" ") || this.token.length() != 70) {
				throw new ConfigurationFileException("Token is malformed.");
			}

			this.fetchInterval = Duration.ofMinutes(Integer.parseInt(properties.getProperty("fetchInterval").strip()));
			this.realityAPI = new URI(properties.getProperty("serverInfoAPI"));

			var serverNames = properties.getProperty("serverNames").split(" ");
			var serverIds = properties.getProperty("serverIds").split(" ");

			if (serverIds.length != serverNames.length) {
				throw new ConfigurationFileException("Number of server names and ids don't match.");
			}

			for (int i = 0; i < serverIds.length; i++) {
				this.servers.add(new TrackedServer(serverNames[i].replace("_", " "), serverIds[i]));
			}

		} catch (FileNotFoundException e) {
			throw new ConfigurationFileException("No configuration file found.");
		} catch (URISyntaxException e) {
			throw new ConfigurationFileException("ServerInfoAPI is not a valid URL.");
		} catch (NumberFormatException e) {
			throw new ConfigurationFileException("Fetch interval is not a number.");
		} catch (IllegalArgumentException e) {
			throw new ConfigurationFileException("Contains malformed Unicode escapes.");
		} catch (ArithmeticException e) {
			throw new ConfigurationFileException("Fetch interval is too large.");
		} catch (IOException e) {
			throw new ConfigurationFileException("Unknown problem ocurred while reading the file.");
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
