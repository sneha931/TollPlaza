package com.freightfox.tollplaza.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;

import com.freightfox.tollplaza.exception.ExternalServiceException;
import com.freightfox.tollplaza.geo.GeoPoint;

/**
 * Geocodes pincodes from a bundled India Post pincode directory, and asks a fallback provider
 * (Nominatim) only for pincodes the directory doesn't contain.
 * <p>
 * The directory covers ~19,250 geographic pincodes (Army Postal Service 9xxxxx codes are excluded),
 * so almost every request is answered locally: no network call, no rate limit, and it works from
 * cloud hosts whose IPs public geocoders block.
 */
public class PincodeDirectoryGeocodingClient implements GeocodingClient {

	private static final Logger log = LoggerFactory.getLogger(PincodeDirectoryGeocodingClient.class);

	private final Map<String, GeoPoint> directory;
	private final GeocodingClient fallback;

	public PincodeDirectoryGeocodingClient(Map<String, GeoPoint> directory, GeocodingClient fallback) {
		this.directory = Map.copyOf(directory);
		this.fallback = fallback;
	}

	@Override
	@Cacheable(cacheNames = "pincodeLocations")
	public Optional<GeoPoint> locate(String pincode) {
		GeoPoint known = directory.get(pincode);
		if (known != null) {
			return Optional.of(known);
		}
		log.debug("Pincode {} not in the bundled directory, asking the fallback geocoder", pincode);
		try {
			return fallback.locate(pincode);
		}
		catch (ExternalServiceException e) {
			// The directory already holds every geographic pincode, so a miss is almost certainly an
			// invalid pincode: report it as unknown rather than failing the request with a 502.
			log.warn("Fallback geocoder unavailable for pincode {} ({}); treating it as unknown", pincode,
					e.getMessage());
			return Optional.empty();
		}
	}

	public int size() {
		return directory.size();
	}

	/**
	 * Reads {@code pincode,latitude,longitude} lines (with a header row). Malformed lines are skipped.
	 */
	public static Map<String, GeoPoint> readDirectory(Resource csv) {
		Map<String, GeoPoint> points = new HashMap<>();
		int skipped = 0;
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(csv.getInputStream(), StandardCharsets.UTF_8))) {
			reader.readLine(); // header
			String line;
			while ((line = reader.readLine()) != null) {
				String[] fields = line.split(",");
				try {
					if (fields.length != 3) {
						throw new IllegalArgumentException("expected 3 fields");
					}
					points.put(fields[0].strip(), new GeoPoint(
							Double.parseDouble(fields[1]), Double.parseDouble(fields[2])));
				}
				catch (IllegalArgumentException e) {
					skipped++;
				}
			}
		}
		catch (IOException e) {
			throw new UncheckedIOException("Cannot read pincode directory " + csv, e);
		}
		log.info("Loaded {} pincode locations from {} ({} malformed lines skipped)", points.size(), csv, skipped);
		return points;
	}
}
