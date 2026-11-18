package com.freightfox.tollplaza.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.freightfox.tollplaza.client.GeocodingClient;
import com.freightfox.tollplaza.client.RoutingClient;
import com.freightfox.tollplaza.config.TollProperties;
import com.freightfox.tollplaza.domain.TollPlaza;
import com.freightfox.tollplaza.dto.TollPlazaResponse;
import com.freightfox.tollplaza.dto.TollPlazaResponse.RouteInfo;
import com.freightfox.tollplaza.dto.TollPlazaResponse.TollPlazaInfo;
import com.freightfox.tollplaza.exception.InvalidPincodeException;
import com.freightfox.tollplaza.exception.RouteNotFoundException;
import com.freightfox.tollplaza.exception.SamePincodeException;
import com.freightfox.tollplaza.geo.BoundingBox;
import com.freightfox.tollplaza.geo.GeoPoint;
import com.freightfox.tollplaza.geo.Route;
import com.freightfox.tollplaza.geo.RouteMatcher;
import com.freightfox.tollplaza.repository.TollPlazaRepository;

/**
 * Orchestrates: pincodes → coordinates → route → toll plazas on that route.
 */
@Service
public class TollPlazaService {

	private static final Logger log = LoggerFactory.getLogger(TollPlazaService.class);

	private final GeocodingClient geocodingClient;
	private final RoutingClient routingClient;
	private final TollPlazaRepository repository;
	private final double maxDistanceFromRouteMeters;
	private final double duplicateRadiusMeters;

	public TollPlazaService(GeocodingClient geocodingClient, RoutingClient routingClient,
			TollPlazaRepository repository, TollProperties properties) {
		this.geocodingClient = geocodingClient;
		this.routingClient = routingClient;
		this.repository = repository;
		this.maxDistanceFromRouteMeters = properties.matching().maxDistanceFromRouteMeters();
		this.duplicateRadiusMeters = properties.matching().duplicateRadiusMeters();
	}

	@Cacheable(cacheNames = "tollPlazaRoutes", key = "#sourcePincode + '->' + #destinationPincode")
	@Transactional(readOnly = true)
	public TollPlazaResponse findTollPlazas(String sourcePincode, String destinationPincode) {
		if (sourcePincode.equals(destinationPincode)) {
			throw new SamePincodeException();
		}
		GeoPoint source = geocodingClient.locate(sourcePincode).orElseThrow(InvalidPincodeException::new);
		GeoPoint destination = geocodingClient.locate(destinationPincode).orElseThrow(InvalidPincodeException::new);

		Route route = routingClient.route(source, destination).orElseThrow(RouteNotFoundException::new);

		BoundingBox box = BoundingBox.around(route.path(), maxDistanceFromRouteMeters);
		List<TollPlaza> candidates = repository.findByLatitudeBetweenAndLongitudeBetween(
				box.minLat(), box.maxLat(), box.minLon(), box.maxLon());

		List<RouteMatcher.Match<TollPlaza>> matches = new RouteMatcher(route, maxDistanceFromRouteMeters)
				.matchAll(candidates, TollPlaza::location);
		List<TollPlazaInfo> plazas = RouteMatcher
				.mergeNearDuplicates(matches, TollPlaza::location, duplicateRadiusMeters)
				.stream()
				.map(m -> new TollPlazaInfo(m.item().getName(), m.item().getLatitude(), m.item().getLongitude(),
						toKm(m.distanceFromStartMeters())))
				.toList();

		log.info("{} -> {}: {} km, {} candidate plazas in bbox, {} on route", sourcePincode, destinationPincode,
				toKm(route.distanceMeters()), candidates.size(), plazas.size());

		return new TollPlazaResponse(
				new RouteInfo(sourcePincode, destinationPincode, toKm(route.distanceMeters())), plazas);
	}

	/** Metres → kilometres, rounded to 2 decimal places. */
	private static double toKm(double meters) {
		return Math.round(meters / 10.0) / 100.0;
	}
}
