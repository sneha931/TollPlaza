package com.freightfox.tollplaza.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.freightfox.tollplaza.client.GeocodingClient;
import com.freightfox.tollplaza.client.MapplsRoutingClient;
import com.freightfox.tollplaza.client.NominatimGeocodingClient;
import com.freightfox.tollplaza.client.OsrmRoutingClient;
import com.freightfox.tollplaza.client.PincodeDirectoryGeocodingClient;
import com.freightfox.tollplaza.client.RoutingClient;

/**
 * Wires the provider clients, so the rest of the app only ever sees {@link GeocodingClient} and
 * {@link RoutingClient}. Geocoding = bundled pincode directory with Nominatim as fallback (decorator);
 * the routing provider is chosen from {@code toll.routing.provider} (factory).
 */
@Configuration
public class HttpClientConfig {

	private static final Logger log = LoggerFactory.getLogger(HttpClientConfig.class);

	@Bean
	GeocodingClient geocodingClient(TollProperties props, ResourceLoader resources) {
		TollProperties.Geocoding geocoding = props.geocoding();
		RestClient nominatim = RestClient.builder()
				.baseUrl(geocoding.baseUrl())
				.defaultHeader(HttpHeaders.USER_AGENT, geocoding.userAgent())
				.requestFactory(requestFactory(props))
				.build();
		return new PincodeDirectoryGeocodingClient(
				PincodeDirectoryGeocodingClient.readDirectory(resources.getResource(geocoding.pincodeDirectory())),
				new NominatimGeocodingClient(nominatim));
	}

	@Bean
	RoutingClient routingClient(TollProperties props) {
		TollProperties.Routing routing = props.routing();
		if ("mappls".equalsIgnoreCase(routing.provider())) {
			if (routing.mappls().hasApiKey()) {
				log.info("Routing provider: Mappls");
				return new MapplsRoutingClient(restClient(routing.mappls().baseUrl(), props),
						routing.mappls().apiKey());
			}
			log.warn("toll.routing.provider=mappls but MAPPLS_API_KEY is not set; falling back to OSRM");
		}
		else if (!"osrm".equalsIgnoreCase(routing.provider())) {
			throw new IllegalStateException("Unknown toll.routing.provider '" + routing.provider()
					+ "' (expected 'mappls' or 'osrm')");
		}
		log.info("Routing provider: OSRM ({})", routing.osrm().baseUrl());
		return new OsrmRoutingClient(restClient(routing.osrm().baseUrl(), props));
	}

	private static RestClient restClient(String baseUrl, TollProperties props) {
		return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory(props)).build();
	}

	private static SimpleClientHttpRequestFactory requestFactory(TollProperties props) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(props.http().connectTimeout());
		factory.setReadTimeout(props.http().readTimeout());
		return factory;
	}
}
