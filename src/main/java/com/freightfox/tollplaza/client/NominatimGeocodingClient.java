package com.freightfox.tollplaza.client;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.freightfox.tollplaza.exception.ExternalServiceException;
import com.freightfox.tollplaza.geo.GeoPoint;

/**
 * Geocodes pincodes using OpenStreetMap Nominatim (free, no API key).
 * <p>
 * Results are cached because Nominatim's usage policy allows at most one request per second.
 */
@Component
public class NominatimGeocodingClient implements GeocodingClient {

	private final RestClient restClient;

	public NominatimGeocodingClient(@Qualifier("geocodingRestClient") RestClient restClient) {
		this.restClient = restClient;
	}

	@Override
	@Cacheable(cacheNames = "pincodeLocations")
	public Optional<GeoPoint> locate(String pincode) {
		List<NominatimPlace> places;
		try {
			places = restClient.get()
					.uri(uri -> uri.path("/search")
							.queryParam("postalcode", pincode)
							.queryParam("countrycodes", "in")
							.queryParam("format", "jsonv2")
							.queryParam("limit", 1)
							.build())
					.retrieve()
					.body(new ParameterizedTypeReference<List<NominatimPlace>>() {
					});
		}
		catch (RestClientException e) {
			throw new ExternalServiceException("Geocoding service is unavailable", e);
		}
		if (places == null || places.isEmpty()) {
			return Optional.empty();
		}
		NominatimPlace place = places.get(0);
		return Optional.of(new GeoPoint(Double.parseDouble(place.lat()), Double.parseDouble(place.lon())));
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record NominatimPlace(String lat, String lon) {
	}
}
