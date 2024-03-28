package me.undermon.maplogger;

import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteDataSource;

import me.undermon.maplogger.ConfigFile.InvalidConfigurationFile;

public class Application {
	private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
	static Logger LOGGER = LoggerFactory.getLogger("MapLogger");

	public static void main(String[] args) {
		try {
			ConfigFile config = ConfigFile.read();
			DataSource dataSource = setupDatabase();

			MapLogger mapLogger = new MapLogger(config, dataSource);

			executor.scheduleWithFixedDelay(mapLogger, 0, config.fetchInterval().getSeconds(), TimeUnit.SECONDS);

		} catch (InvalidConfigurationFile e) {
			LOGGER.error("Invalid configuration file");
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}
	}

	private static DataSource setupDatabase() throws SQLException {
		var dataSource = new SQLiteDataSource();
		dataSource.setUrl("jdbc:sqlite:maplog.db");

		createDatabaseSchema(dataSource);
		
		return dataSource;
	}

	private static void createDatabaseSchema(DataSource dataSource) throws SQLException {
		final String sql = """
			CREATE TABLE IF NOT EXISTS history (
				id INTEGER PRIMARY KEY,
				server TEXT NOT NULL,
				map TEXT NOT NULL,
				mode TEXT NOT NULL,
				layer TEXT NOT NULL,
				players INTEGER NOT NULL,
				timestamp TEXT NOT NULL
			);
			""";

		try (var statement = dataSource.getConnection().prepareStatement(sql)) {
			statement.execute();
		}
	}

	private static void startBot(String token) throws Exception {
		DiscordApi api = new DiscordApiBuilder().setToken(token).setAllIntents().login().join();

		new SlashCommandBuilder().setName("listar");
	}
}
