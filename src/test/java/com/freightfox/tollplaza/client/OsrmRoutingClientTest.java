package com.freightfox.tollplaza.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
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
import com.freightfox.tollplaza.geo.Route;

class OsrmRoutingClientTest {

	private static final GeoPoint FROM = new GeoPoint(13.14, 77.57);
	private static final GeoPoint TO = new GeoPoint(18.56, 73.78);

	private MockRestServiceServer server;
	private OsrmRoutingClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://osrm.test");
		server = MockRestServiceServer.bindTo(builder).build();
		client = new OsrmRoutingClient(builder.build());
	}

	@Test
	void parsesRouteAndConvertsLonLatToLatLon() {
		server.expect(requestTo(Matchers.startsWith("http://osrm.test/route/v1/driving/77.570000,13.140000;73.780000,18.560000")))
				.andExpect(queryParam("overview", "full"))
				.andExpect(queryParam("geometries", "geojson"))
				.andRespond(withSuccess("""
						{"code":"Ok","routes":[{"distance":840123.4,
						  "geometry":{"type":"LineString","coordinates":[[77.57,13.14],[75.0,15.0],[73.78,18.56]]}}]}
						""", MediaType.APPLICATION_JSON));

		Route route = client.route(FROM, TO).orElseThrow();

		assertThat(route.distanceMeters()).isEqualTo(840123.4);
		assertThat(route.path()).containsExactly(new GeoPoint(13.14, 77.57), new GeoPoint(15.0, 75.0),
				new GeoPoint(18.56, 73.78));
		server.verify();
	}

	@Test
	void returnsEmptyWhenOsrmReportsNoRoute() {
		server.expect(requestTo(Matchers.startsWith("http://osrm.test/route")))
				.andRespond(withSuccess("{\"code\":\"NoRoute\",\"routes\":[]}", MediaType.APPLICATION_JSON));

		assertThat(client.route(FROM, TO)).isEmpty();
	}

	@Test
	void returnsEmptyOnOsrm400() {
		server.expect(requestTo(Matchers.startsWith("http://osrm.test/route")))
				.andRespond(withBadRequest().body("{\"code\":\"NoSegment\"}").contentType(MediaType.APPLICATION_JSON));

		assertThat(client.route(FROM, TO)).isEmpty();
	}

	@Test
	void wrapsServerErrorAsExternalServiceException() {
		server.expect(requestTo(Matchers.startsWith("http://osrm.test/route"))).andRespond(withServerError());

		assertThatThrownBy(() -> client.route(FROM, TO)).isInstanceOf(ExternalServiceException.class);
	}
}
