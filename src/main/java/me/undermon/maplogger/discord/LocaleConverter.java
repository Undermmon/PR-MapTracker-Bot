package me.undermon.maplogger.discord;

import java.util.Locale;

import org.javacord.api.interaction.DiscordLocale;

public final class LocaleConverter {

	private LocaleConverter() {
		// Do nothing
	}

	public static Locale fromDiscord(DiscordLocale locale) {
		return (locale.equals(DiscordLocale.UNKNOWN)) ?
			Locale.forLanguageTag("es") : Locale.forLanguageTag(locale.getLocaleCode());
	}

}
