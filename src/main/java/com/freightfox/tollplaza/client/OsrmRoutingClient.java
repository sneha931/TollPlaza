package com.freightfox.tollplaza.client;

import java.util.Optional;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.freightfox.tollplaza.exception.ExternalServiceException;
import com.freightfox.tollplaza.geo.GeoPoint;
import com.freightfox.tollplaza.geo.Route;

/**
 * Driving routes from the OSRM HTTP API (public demo server by default, no API key).
 * Requests the full-resolution geometry as GeoJSON so toll plazas can be matched precisely.
 */
public class OsrmRoutingClient implements RoutingClient {

	private final RestClient restClient;

	public OsrmRoutingClient(RestClient restClient) {
		this.restClient = restClient;
	}

	@Override
	public Optional<Route> route(GeoPoint from, GeoPoint to) {
		OsrmRouteResponse response;
		try {
			response = restClient.get()
					// Literal path (not a {template} var) so ',' and ';' are not percent-encoded
					.uri(uri -> uri.path("/route/v1/driving/" + RoutingCoordinates.lonLatPair(from, to))
							.queryParam("overview", "full")
							.queryParam("geometries", "geojson")
							.queryParam("alternatives", "false")
							.queryParam("steps", "false")
							.build())
					.retrieve()
					.body(OsrmRouteResponse.class);
		}
		catch (HttpClientErrorException e) {
			// OSRM answers 400 with code "NoRoute"/"NoSegment" when points can't be connected
			return Optional.empty();
		}
		catch (RestClientException e) {
			throw new ExternalServiceException("Routing service is unavailable", e);
		}
		return response == null ? Optional.empty() : response.bestRoute();
	}
}
