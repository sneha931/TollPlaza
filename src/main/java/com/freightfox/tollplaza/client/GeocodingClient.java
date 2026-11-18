package com.freightfox.tollplaza.client;

import java.util.Optional;

import com.freightfox.tollplaza.geo.GeoPoint;

/**
 * Resolves an Indian pincode to a coordinate. Implementations are swappable (Strategy pattern).
 */
public interface GeocodingClient {

	/**
	 * @return the pincode's location, or empty if the provider does not know the pincode
	 * @throws com.freightfox.tollplaza.exception.ExternalServiceException if the provider is unreachable
	 */
	Optional<GeoPoint> locate(String pincode);
}
