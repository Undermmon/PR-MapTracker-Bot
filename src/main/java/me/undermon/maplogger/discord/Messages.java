/*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at https://mozilla.org/MPL/2.0/.
*/

package me.undermon.maplogger.discord;

import java.util.Locale;
import java.util.ResourceBundle;

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

	public static String messageTooLong(Locale locale) {
		return get("too_long", locale);
	}
	
	public static String problemOnRetrieval(Locale locale) {
		return get("problem_on_retrieval", locale);
	}

	public static String noRoundsFound(Locale locale) {
		return get("no_rounds_found", locale);
	}

	public static String timeConnective(Locale locale) {
		return get("time_connective", locale);
	}

	public static String searchSpamTooBig(Locale locale) {
		return get("search_spam_too_big", locale);
	}

	public static String mapAttachmentName(Locale locale) {
		return get("map_attachment_name", locale);
	}

	public static String timeOptionName(Locale locale) {
		return get("time_option_name", locale);
	}

	public static String timeOptionDesc(Locale locale) {
		return get("time_option_desc", locale);
	}

	public static String unitOptionName(Locale locale) {
		return get("unit_option_name", locale);
	}

	public static String unitOptionDesc(Locale locale) {
		return get("unit_option_desc", locale);
	}

	public static String serverOptionName(Locale locale) {
		return get("server_option_name", locale);
	}

	public static String serverOptionDesc(Locale locale) {
		return get("server_option_desc", locale);
	}

	public static String hourChoiceName(Locale locale) {
		return get("hour_choice_name", locale);
	}

	public static String dayChoiceName(Locale locale) {
		return get("day_choice_name", locale);
	}

	public static String playedCommandName(Locale locale) {
		return get("played_comm_name", locale);
	}

	public static String playedCommandDesc(Locale locale) {
		return get("played_comm_desc", locale);
	}

	public static String timeZoneConnective(Locale locale) {
		return get("timezone_connective", locale);
	}

	private static final String get(String entry, Locale locale) {
		
		try {
			String string = ResourceBundle.getBundle(NAME, locale, CONTROL).
			getString(entry.toLowerCase());
			
			Logger.info("{} / {} / {}", entry, locale.toLanguageTag(), string);

			return string;
		} catch (Exception e) {
			Logger.warn(e.getMessage());
			
			return "";
		}
	}
}
