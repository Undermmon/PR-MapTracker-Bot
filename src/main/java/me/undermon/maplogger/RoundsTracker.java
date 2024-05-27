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
import me.undermon.maplogger.configuration.MonitoredServer;
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
			var request = HttpRequest.newBuilder().uri(this.config.realitymodAPI()).GET().timeout(TIMEOUT).build();
			var response = HTTP_CLIENT.send(request, BodyHandlers.ofString());

			final List<Round> rounds = Servers.from(response.body()).
				stream().
				filter(server -> config.stream().map(MonitoredServer::identifier).toList().contains(server.identifier())).
				map(Round::from).
				toList();

			this.roundRepo.saveAnyNew(rounds);	

			Logger.info("LOGGING");
			// TODO log map

		} catch (HttpTimeoutException e) {
			Logger.warn( 
				"Timed out fetching %s after %s seconds at %s.".formatted(
					config.realitymodAPI(),
					TIMEOUT.toSeconds(),
					LocalDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.LONG))
				)
			);
		} catch (ThreadDeath e) { 
			throw e;
		} catch (Exception e) {
			Logger.error(e.getMessage());
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
