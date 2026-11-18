package com.freightfox.tollplaza.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.freightfox.tollplaza.exception.ExternalServiceException;
import com.freightfox.tollplaza.geo.GeoPoint;

class NominatimGeocodingClientTest {

	private MockRestServiceServer server;
	private NominatimGeocodingClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://nominatim.test");
		server = MockRestServiceServer.bindTo(builder).build();
		client = new NominatimGeocodingClient(builder.build());
	}

	@Test
	void resolvesKnownPincode() {
		server.expect(requestTo(Matchers.startsWith("http://nominatim.test/search")))
				.andExpect(queryParam("postalcode", "560064"))
				.andExpect(queryParam("countrycodes", "in"))
				.andRespond(withSuccess("[{\"lat\":\"13.1401693\",\"lon\":\"77.5722511\",\"name\":\"560064\"}]",
						MediaType.APPLICATION_JSON));

		assertThat(client.locate("560064")).contains(new GeoPoint(13.1401693, 77.5722511));
		server.verify();
	}

	@Test
	void returnsEmptyForUnknownPincode() {
		server.expect(requestTo(Matchers.startsWith("http://nominatim.test/search")))
				.andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

		assertThat(client.locate("999999")).isEmpty();
	}

	@Test
	void wrapsServerErrorAsExternalServiceException() {
		server.expect(requestTo(Matchers.startsWith("http://nominatim.test/search"))).andRespond(withServerError());

		assertThatThrownBy(() -> client.locate("560064")).isInstanceOf(ExternalServiceException.class);
	}
}
