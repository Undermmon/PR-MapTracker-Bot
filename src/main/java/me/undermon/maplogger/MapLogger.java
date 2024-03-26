package me.undermon.maplogger;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;

import me.undermon.realityapi.Servers;

public class MapLogger implements Runnable {
	private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

	private ConfigFile config;

	public MapLogger(ConfigFile config) {
		this.config = config;
	}

	@Override
	public void run() {
		try {
			HttpRequest request = HttpRequest.newBuilder().uri(this.config.realitymodAPI()).GET().timeout(Duration.ofSeconds(10)).build();
			
			HttpResponse<String> response = HTTP_CLIENT.send(request, BodyHandlers.ofString());
			
			String serverInfo = response.body();

			// serverInfo = Files.readString(Paths.get("ServerInfo.json"));

			List<String> ids = config.stream().map(t -> t.identifier()).toList();
			Servers servers = Servers.from(serverInfo);

			servers.stream().filter(t -> ids.contains(t.identifier())).toList().forEach(t -> System.out.println(t));

		} catch (HttpTimeoutException | InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
