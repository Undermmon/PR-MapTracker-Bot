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
import org.tinylog.Logger;

import me.undermon.maplogger.Round;
import me.undermon.maplogger.RoundRepository;
import me.undermon.maplogger.configuration.Configuration;
import me.undermon.maplogger.configuration.MonitoredServer;
import me.undermon.realityapi.Map;

public final class PlayedCommand implements SlashCommandCreateListener, AutocompleteCreateListener {
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

				Locale locale = LocaleConverter.fromDiscord(command.getLocale());

				respondLater.thenAccept(original -> 
					original.setContent("📎 " + Messages.messageTooLong(locale)).
						addAttachment(stripped.getBytes(), Messages.mapAttachmentName(locale) + ".txt").
						update()
				);
			}

			respondLater.thenAccept(original -> original.setContent(formatedRounds).update());

		} catch (Exception e) {
			Logger.error(e.toString());

			e.printStackTrace();
			respondLater.thenAccept(original -> original.setContent(Messages.problemOnRetrieval(LocaleConverter.fromDiscord(
					command.getLocale()))).update());
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
		Locale locale = LocaleConverter.fromDiscord(discordLocale);

		Logger.info("Locale used to format: {}", locale.toLanguageTag());

		StringBuilder builder = new StringBuilder().
			append("🖥️ **").
			append(server.label().toUpperCase()).
			append("** %s ".formatted(Messages.timeZoneConnective(locale))).
			append(configFile.getTimezone().getDisplayName(TextStyle.FULL, locale)).
			append(".\n");

		if (rounds.isEmpty()) {
			builder.append("\n🔸 ").append(Messages.noRoundsFound(locale));
		}

		LocalDate last = null;
		
		for (Round Round : rounds) {
			ZonedDateTime roundStartTime = Round.startTime().withZoneSameInstant(configFile.getTimezone());

			if (!roundStartTime.toLocalDate().equals(last)) {
				builder.
					append("\n🗓️ **").
					append(roundStartTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))).
					append("**\n");
			}
			last = roundStartTime.toLocalDate();

			builder.append("%s **%s (%s, %s)** %s %s%n".formatted(
				(Round.map() == Map.UNKNOWN) ? "🔸" : "🔹",
				(Round.map() == Map.UNKNOWN) ? "???" : Round.map().getFullName(),
				Round.mode().getShortName().toUpperCase(),
				Round.layer().getShortName().toUpperCase(),
				Messages.timeConnective(locale),
				roundStartTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
			));
		}

		return builder.toString();
	}

	private void failIfSearchSpamIsTooBig(SlashCommandInteraction command, Duration search) {
		if (search.compareTo(Duration.of(730, ChronoUnit.DAYS)) > 0) {
			command.createImmediateResponder().
				setContent(Messages.searchSpamTooBig(LocaleConverter.fromDiscord(command.getLocale()))).
				setFlags(MessageFlag.EPHEMERAL).
				respond();
		}
	}

	@Override
	public void onAutocompleteCreate(AutocompleteCreateEvent event) {
		event.getAutocompleteInteraction().respondWithChoices(this.targetServerChoices);
	}

	public static void register(DiscordApi api) {
		final Locale english = LocaleConverter.fromDiscord(DiscordLocale.ENGLISH_US);
		final Locale portuguese = LocaleConverter.fromDiscord(DiscordLocale.PORTUGUESE_BRAZILIAN);
		final Locale spanish = LocaleConverter.fromDiscord(DiscordLocale.SPANISH);

		SlashCommandOption time = new SlashCommandOptionBuilder().
			setType(SlashCommandOptionType.LONG).
			setLongMaxValue(180).
			setMinLength(1).
			setName(TIME_OPTION).
			setDescription(Messages.timeOptionDesc(english)).
			addNameLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, Messages.timeOptionName(portuguese)).
			addDescriptionLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, Messages.timeOptionDesc(portuguese)).
			addNameLocalization(DiscordLocale.SPANISH, Messages.timeOptionName(spanish)).
			addDescriptionLocalization(DiscordLocale.SPANISH, Messages.timeOptionDesc(spanish)).
			build();

		SlashCommandOptionChoice hour = new SlashCommandOptionChoiceBuilder().
			setName(HOURS_CHOICE).
			addNameLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, Messages.hourChoiceName(portuguese)).
			addNameLocalization(DiscordLocale.SPANISH, Messages.hourChoiceName(spanish)).
			setValue(ChronoUnit.HOURS.name()).
			build();

		SlashCommandOptionChoice day = new SlashCommandOptionChoiceBuilder().
			setName(DAYS_CHOICE). 
			addNameLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, Messages.dayChoiceName(portuguese)).
			addNameLocalization(DiscordLocale.SPANISH, Messages.dayChoiceName(spanish)).
			setValue(ChronoUnit.DAYS.name()).
			build();

		SlashCommandOption timeUnit = new SlashCommandOptionBuilder().
			setType(SlashCommandOptionType.STRING).
			addChoice(hour).
			addChoice(day).
			setName(UNIT_OPTION).
			setDescription(Messages.unitOptionDesc(english)).
			addNameLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, Messages.unitOptionName(portuguese)).
			addDescriptionLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, Messages.unitOptionDesc(portuguese)).
			addNameLocalization(DiscordLocale.SPANISH, Messages.unitOptionName(spanish)).
			addDescriptionLocalization(DiscordLocale.SPANISH, Messages.unitOptionDesc(spanish)).
			build();

		SlashCommandOption server = new SlashCommandOptionBuilder().
			setType(SlashCommandOptionType.STRING).
			setRequired(false).
			setName(SERVER_OPTION).
			setDescription(Messages.serverOptionDesc(english)).
			addNameLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, Messages.serverOptionName(portuguese)).
			addDescriptionLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, Messages.serverOptionDesc(portuguese)).
			addNameLocalization(DiscordLocale.SPANISH, Messages.serverOptionName(spanish)).
			addDescriptionLocalization(DiscordLocale.SPANISH, Messages.serverOptionDesc(spanish)).
			setAutocompletable(true).
			build();

		new SlashCommandBuilder().
			setName(COMMAND_NAME).
			setDescription(Messages.playedCommandDesc(english)).
			addNameLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, Messages.playedCommandName(portuguese)).
			addDescriptionLocalization(DiscordLocale.PORTUGUESE_BRAZILIAN, Messages.playedCommandDesc(portuguese)).
			addNameLocalization(DiscordLocale.SPANISH, Messages.playedCommandName(spanish)).
			addDescriptionLocalization(DiscordLocale.SPANISH, Messages.playedCommandDesc(spanish)).
			addOption(time).
			addOption(timeUnit).
			addOption(server).
			createGlobal(api).
			join();
	}
}
