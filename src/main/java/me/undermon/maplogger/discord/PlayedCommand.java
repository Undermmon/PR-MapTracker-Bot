package me.undermon.maplogger.discord;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.sql.DataSource;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.event.interaction.AutocompleteCreateEvent;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.DiscordLocale;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionBuilder;
import org.javacord.api.interaction.SlashCommandOptionChoice;
import org.javacord.api.interaction.SlashCommandOptionChoiceBuilder;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.listener.interaction.AutocompleteCreateListener;
import org.javacord.api.listener.interaction.SlashCommandCreateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.undermon.maplogger.configuration.Configuration;
import me.undermon.maplogger.configuration.MonitoredServer;
import me.undermon.realityapi.Layer;
import me.undermon.realityapi.Map;
import me.undermon.realityapi.Mode;

public class PlayedCommand implements SlashCommandCreateListener, AutocompleteCreateListener {
	private static final Logger LOGGER = LoggerFactory.getLogger("Console");
	private static final String TIME_OPTION = "time";
	private static final String UNIT_OPTION = "unit";
	private static final String SERVER_OPTION = "server";

	private static final String COMMAND_NAME = "played";

	private static final String roundsQuerySQL = """
		SELECT map, mode, layer, players, timestamp FROM history WHERE (
			server = ? AND datetime(timestamp) > datetime(?)
		);
		""";

	private record GameRound(Map map, Mode mode, Layer layer, ZonedDateTime startTime) {

	}

	private final Configuration configFile;
	private final DataSource dataSource;
	private final List<SlashCommandOptionChoice> targetServerChoices;

	public PlayedCommand(Configuration configFile, DataSource dataSource) {
		this.configFile = configFile;
		this.dataSource = dataSource;
		this.targetServerChoices = configFile.stream().
			map(server -> SlashCommandOptionChoice.create(server.label(), server.identifier())).
			toList();
	}

	@Override
	public void onSlashCommandCreate(SlashCommandCreateEvent event) {
		SlashCommandInteraction command = event.getSlashCommandInteraction();
	
		if (!command.getFullCommandName().equals(COMMAND_NAME)) {
			return;
		}
	
		long time = this.getInputTimeOption(command);
		ChronoUnit unit = this.getInputUnitOption(command);
		MonitoredServer server = this.getInputServerOption(command);
		Duration searchSpam = Duration.of(time, unit);

		this.failIfSearchSpamIsTooBig(command, searchSpam);

		var respondLater = command.respondLater(true);

		try {
			List<GameRound> playedRounds = this.queryGameRoundsOnDatabase(server, searchSpam);

			String formatedRounds = this.formatToMessage(command.getLocale(), server, playedRounds);

			if (formatedRounds.length() > 2000) {
				respondLater.thenAccept(original -> original.setContent("TOO BIG").update());
			}

			respondLater.thenAccept(original -> original.setContent(formatedRounds).update());

		} catch (Exception e) {
			String message;

			if (command.getLocale() == DiscordLocale.PORTUGUESE_BRAZILIAN) {
				message = "Não foi possível realizar a busca devido a problemas internos.";
			} else {
				message = "Unable to perform the search due to internal problems.";
			}

			LOGGER.error(e.getMessage());

			respondLater.thenAccept(original -> original.setContent(message).update());
		}
	}

	private long getInputTimeOption(SlashCommandInteraction command) {
		return Math.abs(command.getOptionByName(TIME_OPTION).flatMap(t -> t.getLongValue()).orElse(1L));
	}

	private ChronoUnit getInputUnitOption(SlashCommandInteraction command) {
		return command.getOptionByName(UNIT_OPTION).flatMap(SlashCommandInteractionOption::getStringValue)
				.map(ChronoUnit::valueOf).orElse(ChronoUnit.DAYS);
	}

	private MonitoredServer getInputServerOption(SlashCommandInteraction command) {
		String idFromChoice = command.
			getOptionByName(SERVER_OPTION).
			flatMap(SlashCommandInteractionOption::getStringValue).
			orElse("");
		
		return configFile.stream().
			filter(t -> t.identifier().equals(idFromChoice)).
			findFirst().
			orElse(configFile.getDefaultMonitoredServer());
	}

	private String formatToMessage(DiscordLocale discordLocale, MonitoredServer server, List<GameRound> rounds) {
		Locale locale = Locale.forLanguageTag(discordLocale.getLocaleCode());
		String timeConnector;
		String noRounds;

		if (discordLocale == DiscordLocale.PORTUGUESE_BRAZILIAN) {
			timeConnector = "ás";
			noRounds = "Não foram encontradas partidas.";
		} else {
			timeConnector = "at";
			noRounds = "No rounds were found.";
		}

		StringBuilder builder = new StringBuilder();

		builder.append("🖥️ **").append(server.label().toUpperCase()).append("**\n");

		if (rounds.isEmpty()) {
			builder.append("🔸 ").append(noRounds);
		}

		LocalDate last = null;

		for (GameRound gameRound : rounds) {
			if (!gameRound.startTime().toLocalDate().equals(last)) {
				builder.
					append("\n🗓️ **").
					append(gameRound.startTime().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))).
					append("**\n");
			}

			last = gameRound.startTime().toLocalDate();
			
			builder.append("🔹 **%s (%s, %s)** %s %s %s\n".formatted(
				gameRound.map().getFullName(),
				gameRound.mode().getShortName().toUpperCase(),
				gameRound.layer().getShortName().toUpperCase(),
				timeConnector,
				gameRound.startTime().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)),
				gameRound.startTime().getZone().getDisplayName(TextStyle.SHORT, locale)
			));
		}

		builder.append("-".repeat(2000));
		return builder.toString();
	}

	private List<GameRound> queryGameRoundsOnDatabase(MonitoredServer server, Duration searchSpam) throws SQLException {
		try (var connection = this.dataSource.getConnection()) {
			try (var statement = connection.prepareStatement(roundsQuerySQL)) {

				statement.setString(1, server.identifier());
				statement.setString(2, ZonedDateTime.now().minus(searchSpam).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

				ResultSet results = statement.executeQuery();

				List<GameRound> rounds = new ArrayList<>();

				while (results.next()) {
					rounds.add(new GameRound(
						Map.fromString(results.getString(1)),
						Mode.fromString(results.getString(2)),
						Layer.fromString(results.getString(3)),
						ZonedDateTime.parse(results.getString(5)))
					);
				}

				return rounds;
			}
		}
	}

	private void failIfSearchSpamIsTooBig(SlashCommandInteraction command, Duration search) {
		if (search.compareTo(Duration.of(730, ChronoUnit.DAYS)) > 0) {
			String message;

			if (command.getLocale() == DiscordLocale.PORTUGUESE_BRAZILIAN) {
				message = "Você pode listar o histórico de mapas de até seis meses atrás.";
			} else {
				message = "You can list map history up to six months ago.";
			}

			command.createImmediateResponder().setContent(message).setFlags(MessageFlag.EPHEMERAL).respond();
		}
	}

	@Override
	public void onAutocompleteCreate(AutocompleteCreateEvent event) {
		event.getAutocompleteInteraction().respondWithChoices(this.targetServerChoices);
	}

	public static void register(DiscordApi api) {

		SlashCommandOption time = new SlashCommandOptionBuilder().
			setType(SlashCommandOptionType.LONG).
			setLongMaxValue(180).
			setMinLength(1).
			setName(TIME_OPTION).
			setDescription("List the last played maps in this amount of time.").
			addNameLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, "tempo").
			addDescriptionLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, "Lista os últimos mapas jogadados neste período de tempo.").
			build();

		SlashCommandOptionChoice hour = new SlashCommandOptionChoiceBuilder().
			setName("hours").
			addNameLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, "horas").
			setValue(ChronoUnit.HOURS.name()).
			build();

		SlashCommandOptionChoice day = new SlashCommandOptionChoiceBuilder().
			setName("days"). 
			addNameLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, "dias").
			setValue(ChronoUnit.DAYS.name()).
			build();

		SlashCommandOption timeUnit = new SlashCommandOptionBuilder().
			setType(SlashCommandOptionType.STRING).
			addChoice(hour).
			addChoice(day).
			setName(UNIT_OPTION).
			setDescription("Time unit to be used for the search.").
			addNameLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, "unidade").
			addDescriptionLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, "Unidade de tempo a ser usada na busca.").
			build();

		SlashCommandOption server = new SlashCommandOptionBuilder().
			setType(SlashCommandOptionType.STRING).
			setRequired(false).
			setName(SERVER_OPTION).
			setDescription("List the played maps on this server").
			addNameLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, "servidor").
			addDescriptionLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, "Lista os mapas jogados neste servidor").
			setAutocompletable(true).
			build();

		new SlashCommandBuilder().
			setName(COMMAND_NAME).
			setDescription("View the map history of a server.").
			addNameLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, "jogados").
			addDescriptionLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, "Veja histórico de mapas de um servidor.").
			addOption(time).
			addOption(timeUnit).
			addOption(server).
			createGlobal(api).
			join();
	}
}
