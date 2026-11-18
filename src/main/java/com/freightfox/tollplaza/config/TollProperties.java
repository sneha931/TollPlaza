package com.freightfox.tollplaza.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalised settings bound from the {@code toll.*} keys in application.properties.
 */
@ConfigurationProperties(prefix = "toll")
public record TollProperties(Geocoding geocoding, Routing routing, Http http, Matching matching, Data data) {

	public record Geocoding(String baseUrl, String userAgent) {
	}

	/**
	 * @param provider {@code mappls} or {@code osrm}
	 */
	public record Routing(String provider, Osrm osrm, Mappls mappls) {
	}

	public record Osrm(String baseUrl) {
	}

	/** {@code apiKey} comes from the MAPPLS_API_KEY environment variable; never commit it. */
	public record Mappls(String baseUrl, String apiKey) {

		public boolean hasApiKey() {
			return apiKey != null && !apiKey.isBlank();
		}

		@Override
		public String toString() {
			return "Mappls[baseUrl=" + baseUrl + ", apiKey=" + (hasApiKey() ? "****" : "<unset>") + "]";
		}
	}

	public record Http(Duration connectTimeout, Duration readTimeout) {
	}

	/**
	 * @param maxDistanceFromRouteMeters a plaza farther than this from the route is not on it
	 * @param duplicateRadiusMeters      plazas closer than this to each other count as one
	 */
	public record Matching(double maxDistanceFromRouteMeters, double duplicateRadiusMeters) {
	}

	public record Data(String csvLocation) {
	}
}
