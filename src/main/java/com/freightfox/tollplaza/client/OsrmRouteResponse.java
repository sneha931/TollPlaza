package com.freightfox.tollplaza.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.freightfox.tollplaza.geo.GeoPoint;
import com.freightfox.tollplaza.geo.Route;

/**
 * OSRM-format route response. Both OSRM and the Mappls Directions API return this shape
 * (requested with {@code geometries=geojson}), so the two clients share it.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record OsrmRouteResponse(String code, List<RouteDto> routes) {

	/** The first (best) route, or empty if the provider reported no route. */
	Optional<Route> bestRoute() {
		if (!"Ok".equalsIgnoreCase(code) || routes == null || routes.isEmpty()) {
			return Optional.empty();
		}
		RouteDto best = routes.get(0);
		if (best.geometry() == null || best.geometry().coordinates() == null
				|| best.geometry().coordinates().size() < 2) {
			return Optional.empty();
		}
		List<GeoPoint> path = new ArrayList<>(best.geometry().coordinates().size());
		for (List<Double> lonLat : best.geometry().coordinates()) {
			path.add(new GeoPoint(lonLat.get(1), lonLat.get(0)));
		}
		return Optional.of(new Route(best.distance(), path));
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record RouteDto(double distance, Geometry geometry) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Geometry(List<List<Double>> coordinates) {
	}
}
