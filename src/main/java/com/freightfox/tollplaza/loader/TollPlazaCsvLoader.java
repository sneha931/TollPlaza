package com.freightfox.tollplaza.loader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.freightfox.tollplaza.config.TollProperties;

/**
 * Loads the toll plaza CSV into the database at startup.
 * <p>
 * Rows are de-duplicated in memory first, then written with a single batched
 * {@code MERGE ... KEY(...)} (H2's upsert). Running the loader again is therefore idempotent:
 * existing plazas are updated in place, never duplicated.
 */
@Component
public class TollPlazaCsvLoader implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(TollPlazaCsvLoader.class);

	static final String UPSERT_SQL = """
			MERGE INTO toll_plaza (name, latitude, longitude, state)
			KEY (name, latitude, longitude)
			VALUES (?, ?, ?, ?)
			""";

	private final JdbcTemplate jdbcTemplate;
	private final ResourceLoader resourceLoader;
	private final TollProperties properties;

	public TollPlazaCsvLoader(JdbcTemplate jdbcTemplate, ResourceLoader resourceLoader, TollProperties properties) {
		this.jdbcTemplate = jdbcTemplate;
		this.resourceLoader = resourceLoader;
		this.properties = properties;
	}

	@Override
	public void run(ApplicationArguments args) throws IOException {
		load();
	}

	/** @return number of distinct plazas upserted */
	@Transactional
	public int load() throws IOException {
		Resource resource = resourceLoader.getResource(properties.data().csvLocation());
		List<CsvRow> rows = readDistinctRows(resource);
		List<Object[]> batch = new ArrayList<>(rows.size());
		for (CsvRow r : rows) {
			batch.add(new Object[] { r.name(), r.latitude(), r.longitude(), r.state() });
		}
		jdbcTemplate.batchUpdate(UPSERT_SQL, batch);
		log.info("Upserted {} distinct toll plazas from {}", rows.size(), resource.getDescription());
		return rows.size();
	}

	List<CsvRow> readDistinctRows(Resource resource) throws IOException {
		Map<String, CsvRow> distinct = new LinkedHashMap<>();
		int lineNo = 0;
		int skipped = 0;
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
			String header = reader.readLine();
			if (header == null) {
				return List.of();
			}
			lineNo++;
			String line;
			while ((line = reader.readLine()) != null) {
				lineNo++;
				if (line.isBlank()) {
					continue;
				}
				try {
					CsvRow row = CsvRow.from(CsvLineParser.parse(line));
					distinct.putIfAbsent(row.naturalKey(), row);
				}
				catch (IllegalArgumentException e) {
					skipped++;
					log.warn("Skipping malformed CSV line {}: {}", lineNo, e.getMessage());
				}
			}
		}
		log.info("Read {} data lines, {} distinct plazas, {} skipped", lineNo - 1, distinct.size(), skipped);
		return new ArrayList<>(distinct.values());
	}

	/** One CSV record. Column order in the file: longitude, latitude, toll_name, geo_state. */
	record CsvRow(String name, double latitude, double longitude, String state) {

		static CsvRow from(List<String> fields) {
			if (fields.size() < 3) {
				throw new IllegalArgumentException("expected at least 3 columns, got " + fields.size());
			}
			double longitude = parseDouble(fields.get(0), "longitude");
			double latitude = parseDouble(fields.get(1), "latitude");
			String name = fields.get(2);
			if (name.isBlank()) {
				throw new IllegalArgumentException("toll name is blank");
			}
			if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
				throw new IllegalArgumentException("coordinates out of range");
			}
			String state = fields.size() > 3 ? fields.get(3) : null;
			return new CsvRow(name, latitude, longitude, state);
		}

		String naturalKey() {
			return name.toLowerCase() + '|' + latitude + '|' + longitude;
		}

		private static double parseDouble(String value, String column) {
			try {
				return Double.parseDouble(value);
			}
			catch (NumberFormatException e) {
				throw new IllegalArgumentException(column + " is not a number: '" + value + "'");
			}
		}
	}
}
