package com.freightfox.tollplaza.client;

import java.util.Optional;

import com.freightfox.tollplaza.geo.GeoPoint;
import com.freightfox.tollplaza.geo.Route;

/**
 * Computes a driving route between two coordinates. Implementations are swappable
 * (e.g. OSRM today, Google Directions later).
 */
public interface RoutingClient {

	/**
	 * @return the route, or empty if no drivable route exists
	 * @throws com.freightfox.tollplaza.exception.ExternalServiceException if the provider is unreachable
	 */
	Optional<Route> route(GeoPoint from, GeoPoint to);
}
