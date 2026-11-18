package com.freightfox.tollplaza.geo;

/**
 * Pure geometry helpers. Stateless and side-effect free so they are trivially unit-testable.
 */
public final class GeoUtils {

	public static final double EARTH_RADIUS_METERS = 6_371_008.8;
	private static final double METERS_PER_DEGREE_LAT = 111_320.0;

	private GeoUtils() {
	}

	/** Great-circle distance between two points, in metres. */
	public static double haversineMeters(GeoPoint a, GeoPoint b) {
		double dLat = Math.toRadians(b.latitude() - a.latitude());
		double dLon = Math.toRadians(b.longitude() - a.longitude());
		double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(a.latitude())) * Math.cos(Math.toRadians(b.latitude()))
						* Math.sin(dLon / 2) * Math.sin(dLon / 2);
		return 2 * EARTH_RADIUS_METERS * Math.asin(Math.min(1, Math.sqrt(h)));
	}

	/**
	 * Projects {@code p} onto segment {@code a→b}.
	 * <p>
	 * Uses a local equirectangular approximation, which is accurate to well under 1% for the
	 * short segments (tens to hundreds of metres) found in a routing polyline.
	 *
	 * @return the perpendicular distance (metres) and the fraction {@code t ∈ [0,1]} along the segment
	 */
	public static SegmentProjection projectOntoSegment(GeoPoint p, GeoPoint a, GeoPoint b) {
		double cosLat = Math.cos(Math.toRadians(a.latitude()));
		double bx = (b.longitude() - a.longitude()) * cosLat;
		double by = b.latitude() - a.latitude();
		double px = (p.longitude() - a.longitude()) * cosLat;
		double py = p.latitude() - a.latitude();

		double lenSq = bx * bx + by * by;
		double t = lenSq == 0 ? 0 : clamp((px * bx + py * by) / lenSq);
		double dx = px - t * bx;
		double dy = py - t * by;
		double distanceMeters = Math.sqrt(dx * dx + dy * dy) * METERS_PER_DEGREE_LAT;
		return new SegmentProjection(distanceMeters, t);
	}

	/** Degrees of latitude spanning the given number of metres. */
	public static double metersToLatDegrees(double meters) {
		return meters / METERS_PER_DEGREE_LAT;
	}

	/** Degrees of longitude spanning the given number of metres at the given latitude. */
	public static double metersToLonDegrees(double meters, double atLatitude) {
		double cos = Math.max(0.01, Math.cos(Math.toRadians(atLatitude)));
		return meters / (METERS_PER_DEGREE_LAT * cos);
	}

	private static double clamp(double v) {
		return Math.max(0, Math.min(1, v));
	}

	public record SegmentProjection(double distanceMeters, double fraction) {
	}
}
