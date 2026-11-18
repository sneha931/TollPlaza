package com.freightfox.tollplaza.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class GeoUtilsTest {

	private static final GeoPoint DELHI = new GeoPoint(28.6139, 77.2090);
	private static final GeoPoint MUMBAI = new GeoPoint(19.0760, 72.8777);

	@Test
	void haversineMatchesKnownCityDistance() {
		// Straight-line Delhi–Mumbai is ~1,150 km
		assertThat(GeoUtils.haversineMeters(DELHI, MUMBAI)).isCloseTo(1_150_000, within(10_000.0));
	}

	@Test
	void haversineOfSamePointIsZero() {
		assertThat(GeoUtils.haversineMeters(DELHI, DELHI)).isZero();
	}

	@Test
	void pointOnSegmentHasZeroDistanceAndCorrectFraction() {
		GeoPoint a = new GeoPoint(12.0, 77.0);
		GeoPoint b = new GeoPoint(12.0, 77.1);
		GeoUtils.SegmentProjection proj = GeoUtils.projectOntoSegment(new GeoPoint(12.0, 77.025), a, b);
		assertThat(proj.distanceMeters()).isCloseTo(0, within(0.01));
		assertThat(proj.fraction()).isCloseTo(0.25, within(1e-9));
	}

	@Test
	void perpendicularOffsetIsMeasuredInMeters() {
		GeoPoint a = new GeoPoint(12.0, 77.0);
		GeoPoint b = new GeoPoint(12.0, 77.1);
		// 0.001° of latitude ≈ 111 m north of the segment's midpoint
		GeoUtils.SegmentProjection proj = GeoUtils.projectOntoSegment(new GeoPoint(12.001, 77.05), a, b);
		assertThat(proj.distanceMeters()).isCloseTo(111.3, within(1.0));
		assertThat(proj.fraction()).isCloseTo(0.5, within(1e-6));
	}

	@Test
	void pointBeyondSegmentEndIsClampedToEndpoint() {
		GeoPoint a = new GeoPoint(12.0, 77.0);
		GeoPoint b = new GeoPoint(12.0, 77.1);
		GeoUtils.SegmentProjection proj = GeoUtils.projectOntoSegment(new GeoPoint(12.0, 77.2), a, b);
		assertThat(proj.fraction()).isEqualTo(1.0);
		assertThat(proj.distanceMeters()).isGreaterThan(10_000);
	}

	@Test
	void degenerateSegmentFallsBackToPointDistance() {
		GeoPoint a = new GeoPoint(12.0, 77.0);
		GeoUtils.SegmentProjection proj = GeoUtils.projectOntoSegment(new GeoPoint(12.001, 77.0), a, a);
		assertThat(proj.fraction()).isZero();
		assertThat(proj.distanceMeters()).isCloseTo(111.3, within(1.0));
	}

	@Test
	void boundingBoxIsPaddedOnAllSides() {
		BoundingBox box = BoundingBox.around(java.util.List.of(new GeoPoint(12, 77), new GeoPoint(13, 78)), 1_113.2);
		assertThat(box.minLat()).isCloseTo(11.99, within(1e-4));
		assertThat(box.maxLat()).isCloseTo(13.01, within(1e-4));
		assertThat(box.minLon()).isLessThan(77);
		assertThat(box.maxLon()).isGreaterThan(78);
	}
}
