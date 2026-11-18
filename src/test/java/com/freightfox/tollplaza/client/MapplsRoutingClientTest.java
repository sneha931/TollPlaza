package com.freightfox.tollplaza.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.freightfox.tollplaza.exception.ExternalServiceException;
import com.freightfox.tollplaza.geo.GeoPoint;
import com.freightfox.tollplaza.geo.Route;

class MapplsRoutingClientTest {

	private static final String KEY = "test-secret-key";
	private static final GeoPoint FROM = new GeoPoint(13.14, 77.57);
	private static final GeoPoint TO = new GeoPoint(18.56, 73.78);

	private MockRestServiceServer server;
	private MapplsRoutingClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://mappls.test/route/direction");
		server = MockRestServiceServer.bindTo(builder).build();
		client = new MapplsRoutingClient(builder.build(), KEY);
	}

	@Test
	void sendsKeyAsAccessTokenAndParsesOsrmCompatibleResponse() {
		server.expect(requestTo(Matchers.startsWith(
				"http://mappls.test/route/direction/route_adv/driving/77.570000,13.140000;73.780000,18.560000")))
				.andExpect(queryParam("access_token", KEY))
				.andExpect(queryParam("geometries", "geojson"))
				.andExpect(queryParam("overview", "full"))
				.andRespond(withSuccess("""
						{"code":"Ok","routes":[{"distance":851800.0,"duration":58000,
						  "geometry":{"type":"LineString","coordinates":[[77.57,13.14],[74.5,15.8],[73.78,18.56]]}}]}
						""", MediaType.APPLICATION_JSON));

		Route route = client.route(FROM, TO).orElseThrow();

		assertThat(route.distanceMeters()).isEqualTo(851800.0);
		assertThat(route.path()).containsExactly(new GeoPoint(13.14, 77.57), new GeoPoint(15.8, 74.5),
				new GeoPoint(18.56, 73.78));
		server.verify();
	}

	@Test
	void returnsEmptyWhenNoRoute() {
		server.expect(requestTo(Matchers.startsWith("http://mappls.test")))
				.andRespond(withSuccess("{\"code\":\"NoRoute\",\"routes\":[]}", MediaType.APPLICATION_JSON));

		assertThat(client.route(FROM, TO)).isEmpty();
	}

	@Test
	void returnsEmptyOn400() {
		server.expect(requestTo(Matchers.startsWith("http://mappls.test"))).andRespond(withBadRequest());

		assertThat(client.route(FROM, TO)).isEmpty();
	}

	@Test
	void rejectedKeyBecomesExternalServiceErrorWithoutLeakingKey() {
		server.expect(requestTo(Matchers.startsWith("http://mappls.test")))
				.andRespond(withStatus(HttpStatus.UNAUTHORIZED));

		assertThatThrownBy(() -> client.route(FROM, TO))
				.isInstanceOf(ExternalServiceException.class)
				.hasMessageContaining("API key")
				.hasMessageNotContaining(KEY)
				.hasNoCause();
	}

	@Test
	void quotaExhaustedBecomesExternalServiceError() {
		server.expect(requestTo(Matchers.startsWith("http://mappls.test")))
				.andRespond(withStatus(HttpStatus.FORBIDDEN));

		assertThatThrownBy(() -> client.route(FROM, TO)).isInstanceOf(ExternalServiceException.class);
	}

	@Test
	void serverErrorBecomesExternalServiceErrorWithoutCause() {
		server.expect(requestTo(Matchers.startsWith("http://mappls.test"))).andRespond(withServerError());

		assertThatThrownBy(() -> client.route(FROM, TO))
				.isInstanceOf(ExternalServiceException.class)
				.hasMessage("Routing service is unavailable")
				.hasNoCause();
	}
}
