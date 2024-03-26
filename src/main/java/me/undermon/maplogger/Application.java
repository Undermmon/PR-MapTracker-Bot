package me.undermon.maplogger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;

public class Application {
	private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

	public static void main(String[] args) throws Exception {
		ConfigFile config = ConfigFile.read();
		
		MapLogger logger = new MapLogger(config);


	
		executor.scheduleWithFixedDelay(logger,0, config.fetchInterval().getSeconds(), TimeUnit.SECONDS);
	}

	private static void startBot(String token) throws Exception {
		DiscordApi api = new DiscordApiBuilder().setToken(token).setAllIntents().login().join();
	}
}
