package com.freightfox.tollplaza.client;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.freightfox.tollplaza.exception.ExternalServiceException;
import com.freightfox.tollplaza.geo.GeoPoint;
import com.freightfox.tollplaza.geo.Route;

/**
 * Driving routes from the Mappls (MapmyIndia) Route Directions API, which follows India's
 * national highways and returns an OSRM-compatible response.
 * <p>
 * The API key travels as the {@code access_token} query parameter, so any exception whose
 * message could contain the request URL is never chained or logged; only the status is.
 */
public class MapplsRoutingClient implements RoutingClient {

	private static final Logger log = LoggerFactory.getLogger(MapplsRoutingClient.class);

	private final RestClient restClient;
	private final String apiKey;

	public MapplsRoutingClient(RestClient restClient, String apiKey) {
		this.restClient = restClient;
		this.apiKey = apiKey;
	}

	@Override
	public Optional<Route> route(GeoPoint from, GeoPoint to) {
		OsrmRouteResponse response;
		try {
			response = restClient.get()
					.uri(uri -> uri.path("/route_adv/driving/" + RoutingCoordinates.lonLatPair(from, to))
							.queryParam("geometries", "geojson")
							.queryParam("overview", "full")
							.queryParam("alternatives", "false")
							.queryParam("steps", "false")
							.queryParam("access_token", apiKey)
							.build())
					.retrieve()
					.body(OsrmRouteResponse.class);
		}
		catch (HttpStatusCodeException e) {
			HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
			if (status == HttpStatus.BAD_REQUEST) {
				return Optional.empty(); // points cannot be connected
			}
			log.error("Mappls routing failed with HTTP {}", e.getStatusCode().value());
			if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN) {
				throw new ExternalServiceException("Routing service rejected the API key or its quota is exhausted");
			}
			throw new ExternalServiceException("Routing service is unavailable");
		}
		catch (RestClientException e) {
			// Message may embed the URL (and therefore the key): log only the type.
			log.error("Mappls routing call failed: {}", e.getClass().getSimpleName());
			throw new ExternalServiceException("Routing service is unavailable");
		}
		return response == null ? Optional.empty() : response.bestRoute();
	}
}
