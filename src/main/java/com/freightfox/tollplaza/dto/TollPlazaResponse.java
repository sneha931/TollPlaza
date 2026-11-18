package com.freightfox.tollplaza.dto;

import java.util.List;

/**
 * Response body for {@code POST /api/v1/toll-plazas}. Distances are in kilometres.
 */
public record TollPlazaResponse(RouteInfo route, List<TollPlazaInfo> tollPlazas) {

	public TollPlazaResponse {
		tollPlazas = List.copyOf(tollPlazas);
	}

	public record RouteInfo(String sourcePincode, String destinationPincode, double distanceInKm) {
	}

	public record TollPlazaInfo(String name, double latitude, double longitude, double distanceFromSource) {
	}
}
