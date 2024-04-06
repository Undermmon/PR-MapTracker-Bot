/*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at https://mozilla.org/MPL/2.0/.
*/

package me.undermon.maplogger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import me.undermon.realityapi.Layer;
import me.undermon.realityapi.Map;
import me.undermon.realityapi.Mode;

public class RoundRepository {

	private static final String ID = "id";
	private static final String SERVER = "server";
	private static final String MAP = "map";
	private static final String MODE = "mode";
	private static final String LAYER = "layer";
	private static final String PLAYERS = "players";
	private static final String TIMESTAMP = "timestamp";

	private DataSource dataSource;

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
	
	private static final String queryRoundsByTimeSpamSQL = """
		SELECT %s, %s, %s, %s, %s FROM history WHERE (
			server = ? AND datetime(timestamp) > datetime(?)
		);
		""".formatted(MAP, MODE, LAYER, PLAYERS, TIMESTAMP);

	private static final String queryLastRoundSQL = """
		SELECT %s, %s, %s, %s, %s, %s FROM history WHERE (
			server = ?
		) ORDER BY timestamp DESC LIMIT 1;
		""".formatted(SERVER, MAP, MODE, LAYER, PLAYERS, TIMESTAMP);

	private static final String persistRoundSQL = """
		INSERT INTO history (
			server, map, mode, layer, players, timestamp
		) VALUES (?, ?, ?, ?, ?, ?);
		""";

	public RoundRepository(DataSource dataSource) throws SQLException {
		this.dataSource = dataSource;
		
		try (var statement = dataSource.getConnection().prepareStatement(createTableSQL)) {
			statement.execute();
		}
	}

	public List<Round> queryRoundsOnDatabase(String identifier, Duration searchSpam) throws SQLException {
		try (var connection = this.dataSource.getConnection()) {
			try (var statement = connection.prepareStatement(queryRoundsByTimeSpamSQL)) {
				
				statement.setString(1, identifier);
				statement.setString(2, ZonedDateTime.now().minus(searchSpam).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

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

	public void saveAnyNew(List<Round> rounds) throws SQLException {

		try (var connection = this.dataSource.getConnection()) {
			for (Round round : rounds) {
				Round lastRound = null;

				try (var read = connection.prepareStatement(queryLastRoundSQL)) {
					read.setString(1, round.server());
					ResultSet result = read.executeQuery();

					if (result.next()) {
						lastRound = new Round(
								result.getString(SERVER),
								Map.fromString(result.getString(MAP)),
								Mode.fromString(result.getString(MODE)),
								Layer.fromString(result.getString(LAYER)),
								result.getInt(PLAYERS),
								ZonedDateTime.parse(result.getString(TIMESTAMP))
						);
					}
				}

				if (lastRound == null || !round.sameServerAndLevel(lastRound)) {
					ZonedDateTime timestamp = ZonedDateTime.now();

					try (var statement = connection.prepareStatement(persistRoundSQL)) {
						statement.setString(1, round.server());
						statement.setString(2, round.map().toString());
						statement.setString(3, round.mode().toString());
						statement.setString(4, round.layer().toString());
						statement.setInt(5, round.players());
						statement.setString(6, timestamp.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

						statement.executeUpdate();
					}
				}
			}
		}
	}
}
