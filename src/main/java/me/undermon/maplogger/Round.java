/*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at https://mozilla.org/MPL/2.0/.
*/

package me.undermon.maplogger;

import java.time.ZonedDateTime;

import me.undermon.realityapi.spy.Layer;
import me.undermon.realityapi.spy.Map;
import me.undermon.realityapi.spy.Mode;
import me.undermon.realityapi.spy.Server;


public record Round(String server, Map map, Mode mode, Layer layer, int players, ZonedDateTime startTime) {

	public boolean sameServerAndLevel(Round other) {
		return 
			other != null &&
			other.server.equals(this.server) &&
			other.map.equals(this.map) &&
			other.mode.equals(this.mode) &&
			other.layer.equals(this.layer);
	}

	public static Round from(Server server) {
		return new Round(
			server.identifier(),
			server.map(),
			server.mode(),
			server.layer(),
			server.connected(),
			ZonedDateTime.now()
		);
	}
}