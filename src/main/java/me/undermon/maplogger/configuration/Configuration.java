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
import java.time.temporal.ChronoUnit;
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
	private List<TrackedServer> servers;
	private URI realityAPI;
	private ChronoUnit defaulUnit;
	private int defaultTimespan;

	public static Configuration readFromDisk() {
		return new Configuration();
	}

	private Configuration() {
		try (FileReader fileReader = new FileReader(FILE_NAME)) {
			Properties properties = new Properties();
			properties.load(fileReader);

			this.token = this.parseToken(properties);
			this.fetchInterval = this.parseFetchInterval(properties);
			this.realityAPI = this.parsePRSpyURL(properties);
			this.defaultTimespan = this.parseDefaultTimespan(properties);
			this.defaulUnit = this.parseDefaultUnit(properties);
			this.servers = this.parseTrackedServers(properties);

		} catch (FileNotFoundException e) {
			throw new ConfigurationFileException("No configuration file found.");
		} catch (IllegalArgumentException e) {
			throw new ConfigurationFileException("File contains malformed Unicode escapes.");
		} catch (IOException e) {
			throw new ConfigurationFileException("Unknown problem ocurred while reading the file.");
		}
	}

	private int parseDefaultTimespan(Properties properties) {
		try {
			return Integer.parseInt(properties.getProperty("defaultTimespan").strip());
		} catch (NumberFormatException e) {
			throw new ConfigurationFileException("Default timespan is not a number.");
		}
	}

	private ChronoUnit parseDefaultUnit(Properties properties) {
		try {
			return ChronoUnit.valueOf(properties.getProperty("defaultUnit").toUpperCase().strip());
		} catch (IllegalArgumentException e) {
			throw new ConfigurationFileException("Default unit must be 'days' or 'hours'.");
		}
	}
	
	private URI parsePRSpyURL(Properties properties) throws ConfigurationFileException {
		try {
			return new URI(properties.getProperty("serverInfoAPI"));
		} catch (URISyntaxException e) {
			throw new ConfigurationFileException("ServerInfoAPI is not a valid URL.");
		}
	}

	private List<TrackedServer> parseTrackedServers(Properties properties) {
		var serverNames = properties.getProperty("serverNames").split(" ");
		var serverIds = properties.getProperty("serverIds").split(" ");

		if (serverIds.length != serverNames.length) {
			throw new ConfigurationFileException("Number of server names and ids don't match.");
		}

		List<TrackedServer> trackedServers = new ArrayList<>();

		for (int i = 0; i < serverIds.length; i++) {
			trackedServers.add(new TrackedServer(serverNames[i].replace("_", " "), serverIds[i]));
		}

		return trackedServers;
	}

	private Duration parseFetchInterval(Properties properties) throws ConfigurationFileException{
		try {
			return Duration.ofMinutes(Integer.parseInt(properties.getProperty("fetchInterval").strip()));
		} catch (ArithmeticException e) {
			throw new ConfigurationFileException("Fetch interval is too large.");
 		} catch (NumberFormatException e) {
			throw new ConfigurationFileException("Fetch interval is not a number.");
		}
	}

	private String parseToken(Properties properties) throws ConfigurationFileException {
		String parsedToken = properties.getProperty("token").strip();

		if (parsedToken.isBlank()) {
			throw new ConfigurationFileException("Token must not be blank.");
		}

		if (parsedToken.contains(" ") || parsedToken.length() != 70) {
			throw new ConfigurationFileException("Token is malformed.");
		}

		return parsedToken;
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

	public ChronoUnit defaulUnit() {
		return defaulUnit;
	}

	public int defaultTimespan() {
		return defaultTimespan;
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
