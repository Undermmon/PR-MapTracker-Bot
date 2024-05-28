/*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at https://mozilla.org/MPL/2.0/.
*/

package me.undermon.maplogger.configuration;

public final class ConfigurationFileException extends RuntimeException {

	public ConfigurationFileException() {}

	public ConfigurationFileException(String message) {
		super(message);
	}
}