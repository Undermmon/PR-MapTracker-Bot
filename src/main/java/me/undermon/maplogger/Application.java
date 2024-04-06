/*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at https://mozilla.org/MPL/2.0/.
*/

package me.undermon.maplogger;

import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteDataSource;

import me.undermon.maplogger.configuration.Configuration;
import me.undermon.maplogger.discord.PlayedCommand;

public final class Application {
	private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
	private static final Logger LOGGER = LoggerFactory.getLogger("Console");

	public static void main(String[] args) {
		try {
			Configuration config = Configuration.readFromDisk();
			RoundRepository roundRepo = new RoundRepository(setupDatabase());

			RoundsTracker mapLogger = new RoundsTracker(config, roundRepo);
			executor.scheduleWithFixedDelay(mapLogger, 0, config.fetchInterval().getSeconds(), TimeUnit.SECONDS);

			String invite = startDiscordBot(config, roundRepo);

			LOGGER.info("Started sucessfully, you can invite the bot with: " + invite);
		} catch (Exception e) {
			LOGGER.error(e.toString());

			executor.shutdown();
		}
	}

	private static DataSource setupDatabase() throws SQLException {
		var dataSource = new SQLiteDataSource();
		dataSource.setUrl("jdbc:sqlite:maps.db");
		
		return dataSource;
	}

	private static String startDiscordBot(Configuration configFile, RoundRepository roundRepo) {
		DiscordApi api = new DiscordApiBuilder().
			setToken(configFile.token()).
			setIntents(Intent.GUILDS).
			login().
			join();

		PlayedCommand.register(api);
		PlayedCommand playedCommand = new PlayedCommand(configFile, roundRepo);
		

		api.addSlashCommandCreateListener(playedCommand);
		api.addAutocompleteCreateListener(playedCommand);

		return api.createBotInvite();
	}
}
