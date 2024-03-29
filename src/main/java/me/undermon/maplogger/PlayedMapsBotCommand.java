package me.undermon.maplogger;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import javax.sql.DataSource;

import org.javacord.api.DiscordApi;
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

public class PlayedMapsBotCommand implements SlashCommandCreateListener, AutocompleteCreateListener {
	private static final String TIME_OPTION = "time";
	private static final String UNIT_OPTION = "unit";
	private static final String SERVER_OPTION = "server";

	private static final String COMMAND_NAME = "played";
	private static final Duration MAX_SEARCH = Duration.of(180, ChronoUnit.DAYS);

	private final ConfigFile configFile;
	private final DataSource dataSource;
	
	public PlayedMapsBotCommand(ConfigFile configFile, DataSource dataSource) {
		this.configFile = configFile;
		this.dataSource = dataSource;
	}

	@Override
	public void onSlashCommandCreate(SlashCommandCreateEvent event) {
		SlashCommandInteraction command = event.getSlashCommandInteraction();
	
		if (!command.getFullCommandName().equals(COMMAND_NAME)) {
			return;
		}
	
		long time = Math.abs(command.getOptionByName(TIME_OPTION).flatMap(t -> t.getLongValue()).orElse(1L));
		
		ChronoUnit unit = command.getOptionByName(UNIT_OPTION).
			flatMap(SlashCommandInteractionOption::getStringValue).
			map(ChronoUnit::valueOf).
			orElse(ChronoUnit.DAYS);
		
		String serverIdentifier = command.getOptionByName(SERVER_OPTION).
			flatMap(SlashCommandInteractionOption::getStringValue).
			orElse("");

		Duration search = Duration.of(time, unit);


		if (search.compareTo(MAX_SEARCH) > 0) {
			String message;

			if (command.getLocale() == DiscordLocale.PORTUGUESE_BRAZILIAN) {
				message = "Você pode listar o histórico de mapas de até seis meses atrás.";
			} else {
				message = "You can list map history up to six months ago.";
			}

			command.createImmediateResponder().setContent(message).respond();
		}


		event.getSlashCommandInteraction().createImmediateResponder().setContent(
			time + " / " + unit + " / " + serverIdentifier
		).respond();

	}

	@Override
	public void onAutocompleteCreate(AutocompleteCreateEvent event) {
		event.getAutocompleteInteraction().respondWithChoices(
			configFile.stream().map(
				monitoredServer -> SlashCommandOptionChoice.create(monitoredServer.label(), monitoredServer.identifier())
			).toList()
		);
	}

	static void register(DiscordApi api) {

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
