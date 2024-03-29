package me.undermon.maplogger.discord;

import javax.sql.DataSource;

import org.javacord.api.DiscordApi;
import org.javacord.api.event.interaction.AutocompleteCreateEvent;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.DiscordLocale;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionBuilder;
import org.javacord.api.interaction.SlashCommandOptionChoice;
import org.javacord.api.interaction.SlashCommandOptionChoiceBuilder;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.listener.interaction.AutocompleteCreateListener;
import org.javacord.api.listener.interaction.SlashCommandCreateListener;

import me.undermon.maplogger.ConfigFile;

public class PlayedCommand implements SlashCommandCreateListener, AutocompleteCreateListener {
	private static final String NAME = "played";

	private ConfigFile configFile;
	private DataSource dataSource;

	public PlayedCommand(ConfigFile configFile, DataSource dataSource) {
		this.configFile = configFile;
		this.dataSource = dataSource;
	}

	@Override
	public void onSlashCommandCreate(SlashCommandCreateEvent event) {
		if (event.getSlashCommandInteraction().getFullCommandName().equals(NAME)) {
			event.getSlashCommandInteraction().createImmediateResponder().setContent(Thread.currentThread().getName()).respond();

		}
	}

	@Override
	public void onAutocompleteCreate(AutocompleteCreateEvent event) {
		event.getAutocompleteInteraction().respondWithChoices(
			configFile.stream().map(
				monitoredServer -> SlashCommandOptionChoice.create(monitoredServer.label(), monitoredServer.identifier())
			).toList()
		);
	}

	public static void register(DiscordApi api) {
		

		SlashCommandOption time = new SlashCommandOptionBuilder().
			setType(SlashCommandOptionType.LONG).
			setLongMaxValue(180).
			setMinLength(1).
			setName("time").
			setDescription("List the last played maps in this amount of time.").
			addNameLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, "tempo").
			addDescriptionLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, "Lista os últimos mapas jogadados neste período de tempo.").
			build();

		SlashCommandOptionChoice hour = new SlashCommandOptionChoiceBuilder().
			setName("hours").
			addNameLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, "horas").
			setValue("hour").
			build();

		SlashCommandOptionChoice day = new SlashCommandOptionChoiceBuilder().
			setName("days"). 
			addNameLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, "dias").
			setValue("day").
			build();

		SlashCommandOption timeUnit = new SlashCommandOptionBuilder().
			setType(SlashCommandOptionType.STRING).
			addChoice(hour).
			addChoice(day).
			setName("unit").
			setDescription("Time unit to be used for the search.").
			addNameLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, "unidade").
			addDescriptionLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, "Unidade de tempo a ser usada na busca.").
			build();

		SlashCommandOption server = new SlashCommandOptionBuilder().
			setType(SlashCommandOptionType.STRING).
			setRequired(false).
			setName("server").
			setDescription("List the played maps on this server").
			addNameLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, "servidor").
			addDescriptionLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, "Lista os mapas jogados neste servidor").
			setAutocompletable(true).
			build();

	
		new SlashCommandBuilder().
			setName(NAME).
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
