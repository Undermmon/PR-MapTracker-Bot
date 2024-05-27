/*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at https://mozilla.org/MPL/2.0/.
*/

package me.undermon.maplogger.discord;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

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

import me.undermon.maplogger.Round;
import me.undermon.maplogger.RoundRepository;
import me.undermon.maplogger.configuration.Configuration;
import me.undermon.maplogger.configuration.MonitoredServer;
import me.undermon.realityapi.Map;

public final class PlayedCommand implements SlashCommandCreateListener, AutocompleteCreateListener {
	private static final Logger LOGGER = LoggerFactory.getLogger("Console");
	private static final String TIME_OPTION = "time";
	private static final String UNIT_OPTION = "unit";
	private static final String SERVER_OPTION = "server";
	private static final String HOURS_CHOICE = "hours";
	private static final String DAYS_CHOICE = "days";

	private static final String COMMAND_NAME = "played";

	private final Configuration configFile;
	private final RoundRepository roundRepo;
	private final List<SlashCommandOptionChoice> targetServerChoices;

	public PlayedCommand(Configuration configFile, RoundRepository roundRepo) {
		this.configFile = configFile;
		this.roundRepo = roundRepo;
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
			List<Round> playedRounds = this.roundRepo.queryRoundsOnDatabase(server.identifier(), searchSpam);

			String formatedRounds = this.formatToMessage(command.getLocale(), server, playedRounds);

			if (formatedRounds.length() > 2000) {
				String stripped = formatedRounds.
					replace("*", "").
					replace("🖥️ ", "").
					replace("🗓️ ", "").
					replace("🔸", "-").
					replace("🔹", "-");

				respondLater.thenAccept(original -> 
					original.setContent("📎 " + Messages.messageTooLong(command.getLocale())).
						addAttachment(stripped.getBytes(), Messages.mapAttachmentName(command.getLocale()) + ".txt").
						update()
				);
			}

			respondLater.thenAccept(original -> original.setContent(formatedRounds).update());

		} catch (Exception e) {
			LOGGER.error(e.toString());

			e.printStackTrace();
			respondLater.thenAccept(original -> original.setContent(Messages.problemOnRetrieval(command.getLocale())).update());
		}
	}

	private long getInputTimeOption(SlashCommandInteraction command) {
		return Math.abs(command.getOptionByName(TIME_OPTION).flatMap(t -> t.getLongValue()).orElse(3L));
	}

	private ChronoUnit getInputUnitOption(SlashCommandInteraction command) {
		return command.getOptionByName(UNIT_OPTION).flatMap(SlashCommandInteractionOption::getStringValue)
				.map(ChronoUnit::valueOf).orElse(ChronoUnit.HOURS);
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

	private String formatToMessage(DiscordLocale discordLocale, MonitoredServer server, List<Round> rounds) {
		Locale locale = Locale.forLanguageTag(discordLocale.getLocaleCode());

		StringBuilder builder = new StringBuilder().
			append("🖥️ **").
			append(server.label().toUpperCase()).
			append("** %s ".formatted(Messages.timeZoneConnective(discordLocale))).
			append(configFile.getTimezone().getDisplayName(TextStyle.FULL, locale)).
			append(".\n");

		if (rounds.isEmpty()) {
			builder.append("\n🔸 ").append(Messages.noRoundsFound(discordLocale));
		}

		LocalDate last = null;
		
		for (Round Round : rounds) {
			ZonedDateTime roundStartTime = Round.startTime().withZoneSameInstant(configFile.getTimezone());

			if (!roundStartTime.toLocalDate().equals(last)) {
				builder.
					append("\n🗓️ **").
					append(roundStartTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))).
					append("**\n");
			}
			last = roundStartTime.toLocalDate();

			builder.append("%s **%s (%s, %s)** %s %s%n".formatted(
				(Round.map() == Map.UNKNOWN) ? "🔸" : "🔹",
				(Round.map() == Map.UNKNOWN) ? "???" : Round.map().getFullName(),
				Round.mode().getShortName().toUpperCase(),
				Round.layer().getShortName().toUpperCase(),
				Messages.timeConnective(discordLocale),
				roundStartTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
			));
		}

		return builder.toString();
	}

	private void failIfSearchSpamIsTooBig(SlashCommandInteraction command, Duration search) {
		if (search.compareTo(Duration.of(730, ChronoUnit.DAYS)) > 0) {
			command.createImmediateResponder().
				setContent(Messages.searchSpamTooBig(command.getLocale())).
				setFlags(MessageFlag.EPHEMERAL).
				respond();
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
			setDescription(Messages.timeOptionDesc(DiscordLocale.ENGLISH_US)).
			addNameLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, Messages.timeOptionName(DiscordLocale.PORTUGUESE_BRAZILIAN)).
			addDescriptionLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, Messages.timeOptionDesc(DiscordLocale.PORTUGUESE_BRAZILIAN)).
			addNameLocalization(DiscordLocale.SPANISH, Messages.timeOptionName(DiscordLocale.SPANISH)).
			addDescriptionLocalization(DiscordLocale.SPANISH, Messages.timeOptionDesc(DiscordLocale.SPANISH)).
			build();

		SlashCommandOptionChoice hour = new SlashCommandOptionChoiceBuilder().
			setName(HOURS_CHOICE).
			addNameLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, Messages.hourChoiceName(DiscordLocale.PORTUGUESE_BRAZILIAN)).
			addNameLocalization(DiscordLocale.SPANISH, Messages.hourChoiceName(DiscordLocale.SPANISH)).
			setValue(ChronoUnit.HOURS.name()).
			build();

		SlashCommandOptionChoice day = new SlashCommandOptionChoiceBuilder().
			setName(DAYS_CHOICE). 
			addNameLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, Messages.dayChoiceName(DiscordLocale.PORTUGUESE_BRAZILIAN)).
			addNameLocalization(DiscordLocale.SPANISH, Messages.dayChoiceName(DiscordLocale.SPANISH)).
			setValue(ChronoUnit.DAYS.name()).
			build();

		SlashCommandOption timeUnit = new SlashCommandOptionBuilder().
			setType(SlashCommandOptionType.STRING).
			addChoice(hour).
			addChoice(day).
			setName(UNIT_OPTION).
			setDescription(Messages.unitOptionDesc(DiscordLocale.ENGLISH_US)).
			addNameLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, Messages.unitOptionName(DiscordLocale.PORTUGUESE_BRAZILIAN)).
			addDescriptionLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, Messages.unitOptionDesc(DiscordLocale.PORTUGUESE_BRAZILIAN)).
			addNameLocalization(DiscordLocale.SPANISH, Messages.unitOptionName(DiscordLocale.SPANISH)).
			addDescriptionLocalization(DiscordLocale.SPANISH, Messages.unitOptionDesc(DiscordLocale.SPANISH)).
			build();

		SlashCommandOption server = new SlashCommandOptionBuilder().
			setType(SlashCommandOptionType.STRING).
			setRequired(false).
			setName(SERVER_OPTION).
			setDescription(Messages.serverOptionDesc(DiscordLocale.ENGLISH_US)).
			addNameLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, Messages.serverOptionName(DiscordLocale.PORTUGUESE_BRAZILIAN)).
			addDescriptionLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, Messages.serverOptionDesc(DiscordLocale.PORTUGUESE_BRAZILIAN)).
			addNameLocalization(DiscordLocale.SPANISH, Messages.serverOptionName(DiscordLocale.SPANISH)).
			addDescriptionLocalization(DiscordLocale.SPANISH, Messages.serverOptionDesc(DiscordLocale.SPANISH)).
			setAutocompletable(true).
			build();

		new SlashCommandBuilder().
			setName(COMMAND_NAME).
			setDescription(Messages.playedCommandDesc(DiscordLocale.ENGLISH_US)).
			addNameLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, Messages.playedCommandName(DiscordLocale.PORTUGUESE_BRAZILIAN)).
			addDescriptionLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, Messages.playedCommandDesc(DiscordLocale.PORTUGUESE_BRAZILIAN)).
			addNameLocalization(DiscordLocale.SPANISH, Messages.playedCommandName(DiscordLocale.SPANISH)).
			addDescriptionLocalization(DiscordLocale.SPANISH, Messages.playedCommandDesc(DiscordLocale.SPANISH)).
			addOption(time).
			addOption(timeUnit).
			addOption(server).
			createGlobal(api).
			join();
	}
}
