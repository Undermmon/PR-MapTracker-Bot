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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.tinylog.Logger;

import me.undermon.maplogger.configuration.Configuration;
import me.undermon.maplogger.configuration.TrackedServer;
import me.undermon.realityapi.spy.Servers;


final class RoundsTracker implements Runnable {
	private static final Duration TIMEOUT = Duration.ofSeconds(60);
	private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

	private final Configuration config;
	private final RoundRepository roundRepo;

	public RoundsTracker(Configuration config, RoundRepository roundRepo) {
		this.config = config;
		this.roundRepo = roundRepo;
	}

	@Override
	public void run() {
		try {
			var request = HttpRequest.newBuilder().uri(this.config.serverInfoAPI()).GET().timeout(TIMEOUT).build();
			var response = HTTP_CLIENT.send(request, BodyHandlers.ofString());

			if (response.statusCode() == 200) {
				final List<Round> rounds = Servers.from(response.body()).
					stream().
					filter(server -> config.stream().map(TrackedServer::id).toList().contains(server.identifier())).
					map(Round::from).
					toList();
					
					List<Round> saveRounds = this.roundRepo.saveAnyNew(rounds);

					saveRounds.forEach(round -> {

						String serverName = this.config.stream().
							filter(trackedServer -> trackedServer.id().equals(round.server())).
							findFirst().
							map(TrackedServer::name).
							orElse(round.server());


						Logger.info("Map change on '{}' to '{}' {} {} with {} players at {}.", 
								serverName,
								round.map().getFullName(),
								round.mode().getShortName(),
								round.layer().getShortName(),
								round.players(),
								round.startTime().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm z"))
						);
					});

			} else {
				Logger.warn("Response code from PRSPY is {}.", response.statusCode());
			}

		} catch (HttpTimeoutException e) {
			Logger.warn( 
				"Timed out fetching %s after %s seconds at %s.".formatted(
					config.serverInfoAPI(),
					TIMEOUT.toSeconds(),
					LocalDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.LONG))
				)
			);
		} catch (ThreadDeath e) { 
			throw e;
		} catch (Exception e) {
			Logger.error(e);
		}
	}

	public static ThreadFactory threadFactory() {
		return runnable -> {
			Thread thread = Executors.defaultThreadFactory().newThread(runnable);

			thread.setName("RoundsTracker");

			return thread;
		};
	}

}
