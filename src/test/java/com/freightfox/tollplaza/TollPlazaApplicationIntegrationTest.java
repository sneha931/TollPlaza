package com.freightfox.tollplaza;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.freightfox.tollplaza.client.GeocodingClient;
import com.freightfox.tollplaza.client.RoutingClient;
import com.freightfox.tollplaza.dto.TollPlazaResponse;
import com.freightfox.tollplaza.geo.GeoPoint;
import com.freightfox.tollplaza.geo.Route;
import com.freightfox.tollplaza.loader.TollPlazaCsvLoader;
import com.freightfox.tollplaza.repository.TollPlazaRepository;
import com.freightfox.tollplaza.service.TollPlazaService;

/**
 * Boots the full application against the real CSV, with the two network clients mocked out.
 */
@SpringBootTest
class TollPlazaApplicationIntegrationTest {

	/** Distinct (name, latitude, longitude) rows in toll_plaza_india.csv (2,385 raw rows). */
	private static final int DISTINCT_PLAZAS = 1536;

	@Autowired
	TollPlazaRepository repository;
	@Autowired
	TollPlazaCsvLoader loader;
	@Autowired
	TollPlazaService service;
	@Autowired
	CacheManager cacheManager;

	@MockitoBean
	GeocodingClient geocoding;
	@MockitoBean
	RoutingClient routing;

	@BeforeEach
	void clearCaches() {
		cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
	}

	@Test
	void startupLoadsDistinctPlazasFromCsv() {
		assertThat(repository.count()).isEqualTo(DISTINCT_PLAZAS);
	}

	@Test
	void reloadingCsvIsIdempotentUpsert() throws Exception {
		loader.load();
		loader.load();
		assertThat(repository.count()).isEqualTo(DISTINCT_PLAZAS);
	}

	@Test
	void findsRealPlazaFromCsvOnRouteAndCachesResult() {
		// Dhamnod Toll Plaza (MP) is at 23.41805978, 74.99684426 – run a short route straight through it
		GeoPoint src = new GeoPoint(23.40, 74.99684426);
		GeoPoint dst = new GeoPoint(23.44, 74.99684426);
		when(geocoding.locate("454552")).thenReturn(Optional.of(src));
		when(geocoding.locate("454001")).thenReturn(Optional.of(dst));
		when(routing.route(any(), any())).thenReturn(Optional.of(new Route(4_450, List.of(src, dst))));

		TollPlazaResponse first = service.findTollPlazas("454552", "454001");
		TollPlazaResponse second = service.findTollPlazas("454552", "454001");

		// The CSV lists this plaza twice, as "Dhamnod" and "Delanpur", ~70 m apart: expect one result
		assertThat(first.tollPlazas()).singleElement()
				.extracting(TollPlazaResponse.TollPlazaInfo::name)
				.isIn("Dhamnod Toll Plaza", "Delanpur Toll Plaza");
		assertThat(second).isSameAs(first);
		verify(routing, times(1)).route(any(), any());
	}
}
