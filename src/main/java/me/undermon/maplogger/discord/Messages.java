/*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at https://mozilla.org/MPL/2.0/.
*/

package me.undermon.maplogger.discord;

import java.util.Locale;
import java.util.ResourceBundle;

import org.javacord.api.interaction.DiscordLocale;
import org.tinylog.Logger;

public final class Messages {
	private static final String NAME = "messages";
	private static final ResourceBundle.Control CONTROL = new ResourceBundle.Control() {
		@Override
		public Locale getFallbackLocale(String baseName, Locale locale) {
			return Locale.ROOT;
		}
	};

	private Messages() {
		// Do nothing
	}

	public static String messageTooLong(DiscordLocale locale) {
		return get("too_long", locale);
	}
	
	public static String problemOnRetrieval(DiscordLocale locale) {
		return get("problem_on_retrieval", locale);
	}

	public static String noRoundsFound(DiscordLocale locale) {
		return get("no_rounds_found", locale);
	}

	public static String timeConnective(DiscordLocale locale) {
		return get("time_connective", locale);
	}

	public static String searchSpamTooBig(DiscordLocale locale) {
		return get("search_spam_too_big", locale);
	}

	public static String mapAttachmentName(DiscordLocale locale) {
		return get("map_attachment_name", locale);
	}

	public static String timeOptionName(DiscordLocale locale) {
		return get("time_option_name", locale);
	}

	public static String timeOptionDesc(DiscordLocale locale) {
		return get("time_option_desc", locale);
	}

	public static String unitOptionName(DiscordLocale locale) {
		return get("unit_option_name", locale);
	}

	public static String unitOptionDesc(DiscordLocale locale) {
		return get("unit_option_desc", locale);
	}

	public static String serverOptionName(DiscordLocale locale) {
		return get("server_option_name", locale);
	}

	public static String serverOptionDesc(DiscordLocale locale) {
		return get("server_option_desc", locale);
	}

	public static String hourChoiceName(DiscordLocale locale) {
		return get("hour_choice_name", locale);
	}

	public static String dayChoiceName(DiscordLocale locale) {
		return get("day_choice_name", locale);
	}

	public static String playedCommandName(DiscordLocale locale) {
		return get("played_comm_name", locale);
	}

	public static String playedCommandDesc(DiscordLocale locale) {
		return get("played_comm_desc", locale);
	}

	public static String timeZoneConnective(DiscordLocale locale) {
		return get("timezone_connective", locale);
	}

	private static final String get(String entry, DiscordLocale locale) {
		try {
			return ResourceBundle.getBundle(NAME, Locale.forLanguageTag(locale.getLocaleCode()), CONTROL).
				getString(entry.toLowerCase());
		} catch (Exception e) {
			Logger.warn(e.getMessage());
			
			return "";
		}
	}
}
