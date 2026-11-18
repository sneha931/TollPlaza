package com.freightfox.tollplaza.geo;

import java.util.List;

/**
 * Axis-aligned lat/lng box used as a cheap pre-filter before precise polyline matching.
 */
public record BoundingBox(double minLat, double maxLat, double minLon, double maxLon) {

	/** Smallest box containing every point, grown by {@code paddingMeters} on all sides. */
	public static BoundingBox around(List<GeoPoint> points, double paddingMeters) {
		if (points.isEmpty()) {
			throw new IllegalArgumentException("Cannot build a bounding box around zero points");
		}
		double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
		double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;
		for (GeoPoint p : points) {
			minLat = Math.min(minLat, p.latitude());
			maxLat = Math.max(maxLat, p.latitude());
			minLon = Math.min(minLon, p.longitude());
			maxLon = Math.max(maxLon, p.longitude());
		}
		double latPad = GeoUtils.metersToLatDegrees(paddingMeters);
		// Use the latitude farthest from the equator so longitude padding is never too small.
		double widestLat = Math.max(Math.abs(minLat), Math.abs(maxLat));
		double lonPad = GeoUtils.metersToLonDegrees(paddingMeters, widestLat);
		return new BoundingBox(minLat - latPad, maxLat + latPad, minLon - lonPad, maxLon + lonPad);
	}
}
