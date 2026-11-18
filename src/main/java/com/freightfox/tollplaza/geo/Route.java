package com.freightfox.tollplaza.geo;

import java.util.List;

/**
 * A driving route as returned by a routing provider.
 *
 * @param distanceMeters total road distance
 * @param path           ordered polyline points from source to destination
 */
public record Route(double distanceMeters, List<GeoPoint> path) {

	public Route {
		path = List.copyOf(path);
	}
}
