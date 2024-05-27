/*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at https://mozilla.org/MPL/2.0/.
*/

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
