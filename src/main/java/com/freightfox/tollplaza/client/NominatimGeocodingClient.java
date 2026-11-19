package com.freightfox.tollplaza.client;

import java.util.List;
import java.util.Optional;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.freightfox.tollplaza.exception.ExternalServiceException;
import com.freightfox.tollplaza.geo.GeoPoint;

/**
 * Geocodes pincodes using OpenStreetMap Nominatim (free, no API key). Used only as the fallback for
 * pincodes missing from the bundled directory (see {@link PincodeDirectoryGeocodingClient}), whose
 * cache also keeps us within Nominatim's one-request-per-second usage policy.
 */
public class NominatimGeocodingClient implements GeocodingClient {

	private final RestClient restClient;

	public NominatimGeocodingClient(RestClient restClient) {
		this.restClient = restClient;
	}

	@Override
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
