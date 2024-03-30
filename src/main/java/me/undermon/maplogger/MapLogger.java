/*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at https://mozilla.org/MPL/2.0/.
*/

package me.undermon.maplogger;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpTimeoutException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.undermon.maplogger.configuration.Configuration;
import me.undermon.maplogger.configuration.MonitoredServer;
import me.undermon.realityapi.Layer;
import me.undermon.realityapi.Map;
import me.undermon.realityapi.Mode;
import me.undermon.realityapi.Server;
import me.undermon.realityapi.Servers;

final class MapLogger implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger("Console");
	private static final DateTimeFormatter DATETIME_FORMATTER =
		DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.LONG);
	private static final Duration TIMEOUT = Duration.ofSeconds(60);
	private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

	private static final String logMapStatement = """
		INSERT INTO history (
			server, map, mode, layer, players, timestamp
		) VALUES (?, ?, ?, ?, ?, ?);
		""";

	private static final String readStatement = """
		SELECT map, mode, layer, players, timestamp FROM history WHERE (
			server = ?
		) ORDER BY timestamp DESC LIMIT 1;
		""";
	
	private final Configuration config;
	private final DataSource dataSource;

	public MapLogger(Configuration config, DataSource dataSource) {
		this.config = config;
		this.dataSource = dataSource;
	}

	@Override
	public void run() {
		try {
			var request = HttpRequest.newBuilder().uri(this.config.realitymodAPI()).GET().timeout(TIMEOUT).build();
			var response = HTTP_CLIENT.send(request, BodyHandlers.ofString());

			final List<Server> serversToBeLogged = Servers.from(response.body()).
				stream().
				filter(server -> config.stream().map(MonitoredServer::identifier).toList().contains(server.identifier())).
				toList();

			try (var connection = dataSource.getConnection()) {
				for (Server server : serversToBeLogged) {
					logIfMapChanged(connection, server);
				}
			}

		} catch (HttpTimeoutException e) {
			LOGGER.warn( 
				"Timed out fetching %s after %s seconds at %s.".formatted(
					config.realitymodAPI(),
					TIMEOUT.toSeconds(),
					LocalDateTime.now().format(DATETIME_FORMATTER))
			);
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}
	}

	private void logIfMapChanged(Connection connection, Server server) throws SQLException {
		boolean sameMap = false;

		try (var read = connection.prepareStatement(readStatement)) {
			read.setString(1, server.identifier());

			ResultSet result = read.executeQuery();

			if (result.next()) {
				Map map = Map.valueOf(result.getString("map"));
				Mode mode = Mode.valueOf(result.getString("mode"));
				Layer layer = Layer.valueOf(result.getString("layer"));

				if (server.map().equals(map) && server.mode().equals(mode) && server.layer().equals(layer)) {
					sameMap = true;
				}
			}
		}

		if (!sameMap) {
			ZonedDateTime timestamp = ZonedDateTime.now();
			String label = config.stream().filter(ms -> ms.identifier().equals(server.identifier())).findFirst().get().label();

			LOGGER.info("Logged map change on '%s' to %s %s %s with %d players at %s".formatted(
				label, server.map(), server.mode(), server.layer(), server.connected(), 
				timestamp.format(DATETIME_FORMATTER))
			);

			try (var statement = connection.prepareStatement(logMapStatement)) {
				statement.setString(1, server.identifier());
				statement.setString(2, server.map().toString());
				statement.setString(3, server.mode().toString());
				statement.setString(4, server.layer().toString());
				statement.setInt(5, server.connected());
				statement.setString(6, timestamp.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

				statement.executeUpdate();
			}
		}
	}

}
