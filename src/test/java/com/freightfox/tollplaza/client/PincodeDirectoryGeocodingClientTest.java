package com.freightfox.tollplaza.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import com.freightfox.tollplaza.exception.ExternalServiceException;
import com.freightfox.tollplaza.geo.GeoPoint;

class PincodeDirectoryGeocodingClientTest {

	private final GeocodingClient fallback = mock(GeocodingClient.class);

	@Test
	void knownPincodeIsAnsweredLocallyWithoutFallback() {
		GeoPoint bengaluru = new GeoPoint(13.10696, 77.56881);
		var client = new PincodeDirectoryGeocodingClient(Map.of("560064", bengaluru), fallback);

		assertThat(client.locate("560064")).contains(bengaluru);
		verifyNoInteractions(fallback);
	}

	@Test
	void unknownPincodeIsDelegatedToFallback() {
		GeoPoint elsewhere = new GeoPoint(20.0, 80.0);
		when(fallback.locate("999998")).thenReturn(Optional.of(elsewhere));
		var client = new PincodeDirectoryGeocodingClient(Map.of(), fallback);

		assertThat(client.locate("999998")).contains(elsewhere);
		verify(fallback).locate("999998");
	}

	@Test
	void unavailableFallbackMeansUnknownPincodeNotServerError() {
		when(fallback.locate("999998")).thenThrow(new ExternalServiceException("Geocoding service is unavailable"));
		var client = new PincodeDirectoryGeocodingClient(Map.of(), fallback);

		assertThat(client.locate("999998")).isEmpty();
	}

	@Test
	void readsDirectorySkippingHeaderAndMalformedLines() {
		String csv = """
				pincode,latitude,longitude
				560064,13.10696,77.56881
				411045,not-a-number,73.76615
				600042,12.98264
				600043,12.95,80.20
				""";
		Map<String, GeoPoint> directory = PincodeDirectoryGeocodingClient.readDirectory(
				new ByteArrayResource(csv.getBytes(StandardCharsets.UTF_8)));

		assertThat(directory).containsOnlyKeys("560064", "600043");
		assertThat(directory.get("560064")).isEqualTo(new GeoPoint(13.10696, 77.56881));
	}

	@Test
	void bundledDirectoryCoversTheAssignmentPincodes() {
		Map<String, GeoPoint> directory = PincodeDirectoryGeocodingClient.readDirectory(
				new ClassPathResource("data/pincode_locations.csv"));

		assertThat(directory).hasSizeGreaterThan(19_000);
		// Yelahanka, Bengaluru and Baner, Pune
		assertThat(directory.get("560064").latitude()).isCloseTo(13.10, within(0.05));
		assertThat(directory.get("560064").longitude()).isCloseTo(77.57, within(0.05));
		assertThat(directory.get("411045").latitude()).isCloseTo(18.57, within(0.05));
		assertThat(directory.get("411045").longitude()).isCloseTo(73.77, within(0.05));
		assertThat(directory).doesNotContainKey("999999");
	}
}
