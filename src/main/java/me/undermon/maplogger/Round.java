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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((server == null) ? 0 : server.hashCode());
		result = prime * result + ((map == null) ? 0 : map.hashCode());
		result = prime * result + ((mode == null) ? 0 : mode.hashCode());
		result = prime * result + ((layer == null) ? 0 : layer.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Round other = (Round) obj;
		if (server == null) {
			if (other.server != null)
				return false;
		} else if (!server.equals(other.server))
			return false;
		if (map != other.map)
			return false;
		if (mode != other.mode)
			return false;
		if (layer != other.layer)
			return false;
		return true;
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