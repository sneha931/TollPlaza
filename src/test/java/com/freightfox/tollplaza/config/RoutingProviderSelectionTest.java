package com.freightfox.tollplaza.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.freightfox.tollplaza.client.MapplsRoutingClient;
import com.freightfox.tollplaza.client.OsrmRoutingClient;

class RoutingProviderSelectionTest {

	private final HttpClientConfig config = new HttpClientConfig();

	@Test
	void usesMapplsWhenSelectedAndKeyPresent() {
		assertThat(config.routingClient(props("mappls", "some-key"))).isInstanceOf(MapplsRoutingClient.class);
	}

	@Test
	void fallsBackToOsrmWhenMapplsKeyMissing() {
		assertThat(config.routingClient(props("mappls", ""))).isInstanceOf(OsrmRoutingClient.class);
		assertThat(config.routingClient(props("mappls", null))).isInstanceOf(OsrmRoutingClient.class);
	}

	@Test
	void usesOsrmWhenSelected() {
		assertThat(config.routingClient(props("osrm", "ignored"))).isInstanceOf(OsrmRoutingClient.class);
	}

	@Test
	void rejectsUnknownProvider() {
		assertThatThrownBy(() -> config.routingClient(props("bing", null))).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void toStringNeverPrintsTheKey() {
		assertThat(new TollProperties.Mappls("http://x", "super-secret").toString())
				.doesNotContain("super-secret")
				.contains("****");
	}

	private static TollProperties props(String provider, String mapplsKey) {
		return new TollProperties(null,
				new TollProperties.Routing(provider, new TollProperties.Osrm("http://osrm.test"),
						new TollProperties.Mappls("http://mappls.test", mapplsKey)),
				new TollProperties.Http(Duration.ofSeconds(1), Duration.ofSeconds(1)), null, null);
	}
}
