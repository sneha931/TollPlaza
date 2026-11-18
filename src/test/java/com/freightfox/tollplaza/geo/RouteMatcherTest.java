package com.freightfox.tollplaza.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.Test;

class RouteMatcherTest {

	/** An L-shaped route: east along lat 12.0, then north along lon 77.2. */
	private static final Route ROUTE = new Route(0, List.of(
			new GeoPoint(12.0, 77.0),
			new GeoPoint(12.0, 77.1),
			new GeoPoint(12.0, 77.2),
			new GeoPoint(12.1, 77.2)));

	private final RouteMatcher matcher = new RouteMatcher(ROUTE, 500);

	@Test
	void pointNextToRouteIsMatchedWithDistanceAlongRoute() {
		// ~100 m north of the route, halfway along the first segment (~5.44 km from start)
		assertThat(matcher.locate(new GeoPoint(12.0009, 77.05)))
				.hasValueSatisfying(d -> assertThat(d).isCloseTo(5_440, within(30.0)));
	}

	@Test
	void pointFarFromRouteIsRejected() {
		// ~5.5 km south of the route
		assertThat(matcher.locate(new GeoPoint(11.95, 77.05))).isEmpty();
	}

	@Test
	void distanceAlongRouteAccumulatesAcrossSegments() {
		// On the northbound leg: ~21.8 km east + ~5.6 km north
		assertThat(matcher.locate(new GeoPoint(12.05, 77.2)))
				.hasValueSatisfying(d -> assertThat(d).isCloseTo(21_770 + 5_560, within(100.0)));
	}

	@Test
	void matchAllFiltersAndSortsByDistanceFromStart() {
		record Plaza(String name, GeoPoint at) {
		}
		List<Plaza> plazas = List.of(
				new Plaza("far-along", new GeoPoint(12.08, 77.2)),
				new Plaza("off-route", new GeoPoint(11.9, 77.1)),
				new Plaza("near-start", new GeoPoint(12.0, 77.01)));

		List<RouteMatcher.Match<Plaza>> matches = matcher.matchAll(plazas, Plaza::at);

		assertThat(matches).extracting(m -> m.item().name()).containsExactly("near-start", "far-along");
	}

	@Test
	void mergeKeepsPlazasThatAreCloseAlongRouteButFarApartOnGround() {
		GeoPoint a = new GeoPoint(12.0, 77.0);
		GeoPoint b = new GeoPoint(12.0, 77.001); // ~109 m apart: merged at 200 m
		GeoPoint c = new GeoPoint(12.0, 77.05); // ~5 km on: kept
		List<RouteMatcher.Match<GeoPoint>> sorted = List.of(
				new RouteMatcher.Match<>(a, 0), new RouteMatcher.Match<>(b, 109), new RouteMatcher.Match<>(c, 5_440));

		assertThat(RouteMatcher.mergeNearDuplicates(sorted, p -> p, 200))
				.extracting(RouteMatcher.Match::item).containsExactly(a, c);
		assertThat(RouteMatcher.mergeNearDuplicates(sorted, p -> p, 50)).hasSize(3);
	}

	@Test
	void rejectsRouteWithFewerThanTwoPoints() {
		Route single = new Route(0, List.of(new GeoPoint(12, 77)));
		assertThatThrownBy(() -> new RouteMatcher(single, 500)).isInstanceOf(IllegalArgumentException.class);
	}
}
