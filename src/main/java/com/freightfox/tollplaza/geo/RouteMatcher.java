package com.freightfox.tollplaza.geo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Decides which candidate points lie on a route and how far along the route they are.
 * <p>
 * Cumulative distances along the polyline are precomputed once in the constructor, so
 * each {@link #locate} call is a single linear scan over the route segments.
 */
public class RouteMatcher {

	private final List<GeoPoint> path;
	private final double[] cumulativeMeters;
	private final double maxDistanceFromRouteMeters;

	public RouteMatcher(Route route, double maxDistanceFromRouteMeters) {
		if (route.path().size() < 2) {
			throw new IllegalArgumentException("Route must contain at least two points");
		}
		this.path = route.path();
		this.maxDistanceFromRouteMeters = maxDistanceFromRouteMeters;
		this.cumulativeMeters = new double[path.size()];
		for (int i = 1; i < path.size(); i++) {
			cumulativeMeters[i] = cumulativeMeters[i - 1] + GeoUtils.haversineMeters(path.get(i - 1), path.get(i));
		}
	}

	/**
	 * Returns how far along the route {@code point} is (metres from the start), or empty if the
	 * point is farther than the configured threshold from every segment of the route.
	 */
	public Optional<Double> locate(GeoPoint point) {
		double bestDistance = Double.MAX_VALUE;
		double bestAlong = 0;
		for (int i = 0; i < path.size() - 1; i++) {
			GeoUtils.SegmentProjection proj = GeoUtils.projectOntoSegment(point, path.get(i), path.get(i + 1));
			if (proj.distanceMeters() < bestDistance) {
				bestDistance = proj.distanceMeters();
				double segLen = cumulativeMeters[i + 1] - cumulativeMeters[i];
				bestAlong = cumulativeMeters[i] + proj.fraction() * segLen;
			}
		}
		return bestDistance <= maxDistanceFromRouteMeters ? Optional.of(bestAlong) : Optional.empty();
	}

	/**
	 * Filters {@code candidates} down to those on the route, sorted by distance from the start.
	 */
	public <T> List<Match<T>> matchAll(List<T> candidates, Function<T, GeoPoint> locationOf) {
		List<Match<T>> matches = new ArrayList<>();
		for (T candidate : candidates) {
			locate(locationOf.apply(candidate)).ifPresent(along -> matches.add(new Match<>(candidate, along)));
		}
		matches.sort(Comparator.comparingDouble(Match::distanceFromStartMeters));
		return matches;
	}

	/**
	 * Collapses matches that are within {@code radiusMeters} of the previously kept match, both
	 * along the route and on the ground. The reference data lists some plazas twice under
	 * different spellings (e.g. "Khed-Shivapur" / "Khedshivapur", 13 m apart); the first one
	 * along the route is kept.
	 *
	 * @param matches matches sorted by distance from start (as returned by {@link #matchAll})
	 */
	public static <T> List<Match<T>> mergeNearDuplicates(List<Match<T>> matches, Function<T, GeoPoint> locationOf,
			double radiusMeters) {
		List<Match<T>> kept = new ArrayList<>();
		for (Match<T> m : matches) {
			if (!kept.isEmpty()) {
				Match<T> last = kept.get(kept.size() - 1);
				boolean closeAlongRoute = m.distanceFromStartMeters() - last.distanceFromStartMeters() <= radiusMeters;
				boolean closeOnGround = GeoUtils.haversineMeters(locationOf.apply(last.item()),
						locationOf.apply(m.item())) <= radiusMeters;
				if (closeAlongRoute && closeOnGround) {
					continue;
				}
			}
			kept.add(m);
		}
		return kept;
	}

	public record Match<T>(T item, double distanceFromStartMeters) {
	}
}
