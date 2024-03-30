package me.undermon.maplogger;

import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.sql.DataSource;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteDataSource;

import me.undermon.maplogger.configuration.Configuration;
import me.undermon.maplogger.discord.PlayedCommand;

public class Application {
	private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
	private static final Logger LOGGER = LoggerFactory.getLogger("Console");

	public static void main(String[] args) {
		try {
			Configuration config = Configuration.readFromDisk();
			DataSource dataSource = setupDatabase();
			MapLogger mapLogger = new MapLogger(config, dataSource);

			// TODO uncoment this
			// executor.scheduleWithFixedDelay(mapLogger, 0, config.fetchInterval().getSeconds(), TimeUnit.SECONDS);

			String invite = startDiscordBot(config, dataSource);

			LOGGER.info("Started sucessfully, you can invite the bot with: " + invite);
		} catch (Exception e) {
			LOGGER.error(e.toString());

			executor.shutdown();
		}
	}

	private static DataSource setupDatabase() throws SQLException {
		var dataSource = new SQLiteDataSource();
		dataSource.setUrl("jdbc:sqlite:maplog.db"); // TODO Change db name

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

	private static String startDiscordBot(Configuration configFile, DataSource dataSource) {
		DiscordApi api = new DiscordApiBuilder().
			setToken(configFile.token()).
			setIntents(Intent.GUILDS).
			login().
			join();

		PlayedCommand.register(api);
		PlayedCommand playedCommand = new PlayedCommand(configFile, dataSource);
		

		api.addSlashCommandCreateListener(playedCommand);
		api.addAutocompleteCreateListener(playedCommand);

		return api.createBotInvite();
	}
}
