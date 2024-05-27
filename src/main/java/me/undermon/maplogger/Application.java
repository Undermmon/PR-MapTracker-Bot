/*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at https://mozilla.org/MPL/2.0/.
*/

package me.undermon.maplogger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;
import org.tinylog.Logger;

import me.undermon.maplogger.configuration.Configuration;
import me.undermon.maplogger.discord.PlayedCommand;

public final class Application {
	private static final ScheduledExecutorService executor =
		Executors.newSingleThreadScheduledExecutor(RoundsTracker.threadFactory());

	public static void main(String[] args) {
		try {
			Configuration config = Configuration.readFromDisk();
			RoundRepository roundRepo = RoundRepository.usingSQLite();

			executor.scheduleWithFixedDelay(
				new RoundsTracker(config, roundRepo),
				0,
				config.fetchInterval().getSeconds(),
				TimeUnit.SECONDS
			);

			String invite = startDiscordBot(config, roundRepo);

			Logger.info("Started sucessfully, you can invite the bot with: {}", invite);
		} catch (Exception e) {
			Logger.error(e.toString());

			executor.shutdown();
		}
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
