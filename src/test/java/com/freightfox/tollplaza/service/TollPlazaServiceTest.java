package com.freightfox.tollplaza.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.freightfox.tollplaza.client.GeocodingClient;
import com.freightfox.tollplaza.client.RoutingClient;
import com.freightfox.tollplaza.config.TollProperties;
import com.freightfox.tollplaza.domain.TollPlaza;
import com.freightfox.tollplaza.dto.TollPlazaResponse;
import com.freightfox.tollplaza.exception.InvalidPincodeException;
import com.freightfox.tollplaza.exception.RouteNotFoundException;
import com.freightfox.tollplaza.exception.SamePincodeException;
import com.freightfox.tollplaza.geo.GeoPoint;
import com.freightfox.tollplaza.geo.Route;
import com.freightfox.tollplaza.repository.TollPlazaRepository;

@ExtendWith(MockitoExtension.class)
class TollPlazaServiceTest {

	private static final GeoPoint SRC = new GeoPoint(12.0, 77.0);
	private static final GeoPoint DST = new GeoPoint(12.0, 77.2);
	private static final Route ROUTE = new Route(21_770, List.of(SRC, new GeoPoint(12.0, 77.1), DST));

	@Mock
	GeocodingClient geocoding;
	@Mock
	RoutingClient routing;
	@Mock
	TollPlazaRepository repository;

	TollPlazaService service;

	@BeforeEach
	void setUp() {
		TollProperties props = new TollProperties(null, null, null, new TollProperties.Matching(500, 200), null);
		service = new TollPlazaService(geocoding, routing, repository, props);
	}

	@Test
	void returnsPlazasOnRouteSortedByDistanceWithRouteSummary() {
		givenRoute();
		when(repository.findByLatitudeBetweenAndLongitudeBetween(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
				.thenReturn(List.of(
						new TollPlaza("Second", 12.0, 77.15, "KA"),
						new TollPlaza("First", 12.0005, 77.05, "KA"),
						new TollPlaza("Off route", 12.2, 77.1, "KA")));

		TollPlazaResponse response = service.findTollPlazas("560064", "411045");

		assertThat(response.route().sourcePincode()).isEqualTo("560064");
		assertThat(response.route().destinationPincode()).isEqualTo("411045");
		assertThat(response.route().distanceInKm()).isEqualTo(21.77);
		assertThat(response.tollPlazas()).extracting(TollPlazaResponse.TollPlazaInfo::name)
				.containsExactly("First", "Second");
		assertThat(response.tollPlazas().get(0).distanceFromSource()).isBetween(5.3, 5.6);
		assertThat(response.tollPlazas().get(0).latitude()).isEqualTo(12.0005);
	}

	@Test
	void mergesSamePlazaListedUnderTwoNames() {
		givenRoute();
		// Real CSV case: "Khed-Shivapur" and "Khedshivapur" are ~130 m apart
		when(repository.findByLatitudeBetweenAndLongitudeBetween(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
				.thenReturn(List.of(
						new TollPlaza("Khed-Shivapur Toll Plaza", 12.0, 77.1000, "MH"),
						new TollPlaza("Khedshivapur Toll Plaza", 12.0002, 77.1012, "MH"),
						new TollPlaza("Next Plaza", 12.0, 77.15, "MH")));

		TollPlazaResponse response = service.findTollPlazas("560064", "411045");

		assertThat(response.tollPlazas()).extracting(TollPlazaResponse.TollPlazaInfo::name)
				.containsExactly("Khed-Shivapur Toll Plaza", "Next Plaza");
	}

	@Test
	void returnsEmptyListWhenNoPlazasOnRoute() {
		givenRoute();
		when(repository.findByLatitudeBetweenAndLongitudeBetween(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
				.thenReturn(List.of());

		TollPlazaResponse response = service.findTollPlazas("560064", "411045");

		assertThat(response.tollPlazas()).isEmpty();
		assertThat(response.route().distanceInKm()).isEqualTo(21.77);
	}

	@Test
	void rejectsSameSourceAndDestinationWithoutCallingProviders() {
		assertThatThrownBy(() -> service.findTollPlazas("560064", "560064"))
				.isInstanceOf(SamePincodeException.class)
				.hasMessage("Source and destination pincodes cannot be the same");
		verifyNoInteractions(geocoding, routing, repository);
	}

	@Test
	void rejectsUnknownSourcePincode() {
		when(geocoding.locate("999999")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.findTollPlazas("999999", "411045"))
				.isInstanceOf(InvalidPincodeException.class)
				.hasMessage("Invalid source or destination pincode");
		verifyNoInteractions(routing);
	}

	@Test
	void rejectsUnknownDestinationPincode() {
		when(geocoding.locate("560064")).thenReturn(Optional.of(SRC));
		when(geocoding.locate("999999")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.findTollPlazas("560064", "999999"))
				.isInstanceOf(InvalidPincodeException.class);
	}

	@Test
	void reportsMissingRoute() {
		when(geocoding.locate("560064")).thenReturn(Optional.of(SRC));
		when(geocoding.locate("744101")).thenReturn(Optional.of(DST));
		when(routing.route(SRC, DST)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.findTollPlazas("560064", "744101"))
				.isInstanceOf(RouteNotFoundException.class);
	}

	private void givenRoute() {
		when(geocoding.locate("560064")).thenReturn(Optional.of(SRC));
		when(geocoding.locate("411045")).thenReturn(Optional.of(DST));
		when(routing.route(SRC, DST)).thenReturn(Optional.of(ROUTE));
	}
}
