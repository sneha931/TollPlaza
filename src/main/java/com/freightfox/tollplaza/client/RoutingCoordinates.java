package com.freightfox.tollplaza.client;

import java.util.Locale;

import com.freightfox.tollplaza.geo.GeoPoint;

/** Formats coordinates the way OSRM-style APIs expect them in the URL path. */
final class RoutingCoordinates {

	private RoutingCoordinates() {
	}

	/** {@code "lon,lat;lon,lat"} with a locale-independent decimal point. */
	static String lonLatPair(GeoPoint from, GeoPoint to) {
		return String.format(Locale.ROOT, "%f,%f;%f,%f",
				from.longitude(), from.latitude(), to.longitude(), to.latitude());
	}
}
