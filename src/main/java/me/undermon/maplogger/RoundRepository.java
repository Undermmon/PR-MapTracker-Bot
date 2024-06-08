/*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at https://mozilla.org/MPL/2.0/.
*/

package me.undermon.maplogger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.sqlite.SQLiteDataSource;

import me.undermon.realityapi.spy.Layer;
import me.undermon.realityapi.spy.Map;
import me.undermon.realityapi.spy.Mode;


public class RoundRepository {

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
	private static final String ID = "id";
	private static final String SERVER = "server";
	private static final String MAP = "map";
	private static final String MODE = "mode";
	private static final String LAYER = "layer";
	private static final String PLAYERS = "players";
	private static final String TIMESTAMP = "timestamp";

	private final DataSource dataSource;

	final String createTableSQL = """
		CREATE TABLE IF NOT EXISTS history (
		  %s INTEGER PRIMARY KEY,
		  %s TEXT NOT NULL,
		  %s TEXT NOT NULL,
		  %s TEXT NOT NULL,
		  %s TEXT NOT NULL,
		  %s INTEGER NOT NULL,
		  %s TEXT NOT NULL
		);
		""".formatted(ID, SERVER, MAP, MODE, LAYER, PLAYERS, TIMESTAMP);
	
	private static final String ROUNDS_BY_TIMESPAM_SQL = """
		SELECT %s, %s, %s, %s, %s FROM history WHERE (
		  server = ? AND datetime(timestamp) > datetime(?)
		);
		""".formatted(MAP, MODE, LAYER, PLAYERS, TIMESTAMP);

	private static final String SAVE_ROUNDS_SQL = """
		INSERT INTO history (
		  server, map, mode, layer, players, timestamp
		) VALUES (?, ?, ?, ?, ?, ?);
		""";

	private static final String LAST_ROUNDS_SQL = """
			SELECT server, map, mode, layer, players, MAX(timestamp) AS latest_timestamp
			FROM history
			GROUP BY server
			ORDER BY datetime(latest_timestamp) DESC;
			""";

	public RoundRepository(DataSource dataSource) throws SQLException {
		this.dataSource = dataSource;
		
		try (var statement = dataSource.getConnection().prepareStatement(createTableSQL)) {
			statement.execute();
		}
	}

	public List<Round> queryByTimespam(String identifier, Duration searchSpam) throws SQLException {
		try (var connection = this.dataSource.getConnection()) {
			try (var statement = connection.prepareStatement(ROUNDS_BY_TIMESPAM_SQL)) {
				
				statement.setString(1, identifier);
				statement.setString(2, ZonedDateTime.now().minus(searchSpam).format(DATE_TIME_FORMATTER));

				ResultSet results = statement.executeQuery();
				
				List<Round> rounds = new ArrayList<>();
				
				while (results.next()) {
					rounds.add(new Round(
						identifier,
						Map.fromString(results.getString(MAP)),
						Mode.fromString(results.getString(MODE)),
						Layer.fromString(results.getString(LAYER)),
						results.getInt(PLAYERS),
						ZonedDateTime.parse(results.getString(TIMESTAMP)))
					);
				}
				
				return rounds;
			}
		}
	}

	private List<Round> lastRounds(Connection connection) throws SQLException {
		try (var statement = connection.prepareStatement(LAST_ROUNDS_SQL)) {
			List<Round> rounds = new ArrayList<>();

			ResultSet resultSet = statement.executeQuery();

			while (resultSet.next()) {
				rounds.add(
						new Round(
								resultSet.getString(SERVER),
								Map.fromString(resultSet.getString(MAP)),
								Mode.fromString(resultSet.getString(MODE)),
								Layer.fromString(resultSet.getString(LAYER)),
								resultSet.getInt(PLAYERS),
								ZonedDateTime.parse(resultSet.getString("latest_timestamp"), DATE_TIME_FORMATTER)
						)
				);
			}

			return rounds;
		}
	}

	public List<Round> saveAnyNew(List<Round> rounds) throws SQLException {

		try (var connection = this.dataSource.getConnection()) {
			List<Round> lastRounds = this.lastRounds(connection);
			List<Round> savedRounds = new ArrayList<>();

			try (var statement = connection.prepareStatement(SAVE_ROUNDS_SQL)) {
				for (Round round : rounds) {
					if (!lastRounds.contains(round)) {
						savedRounds.add(round);

						statement.setString(1, round.server());
						statement.setString(2, round.map().toString());
						statement.setString(3, round.mode().toString());
						statement.setString(4, round.layer().toString());
						statement.setInt(5, round.players());
						statement.setString(6, round.startTime().format(DATE_TIME_FORMATTER));

						statement.addBatch();
					}
				}

				statement.executeBatch();
			}

			return savedRounds;
		}
	}

	public static RoundRepository usingSQLite() throws SQLException {
		var dataSource = new SQLiteDataSource();
		dataSource.setUrl("jdbc:sqlite:maps.db");

		return new RoundRepository(dataSource);	
	}

}
