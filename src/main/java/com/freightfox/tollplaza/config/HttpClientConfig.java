package com.freightfox.tollplaza.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.freightfox.tollplaza.client.MapplsRoutingClient;
import com.freightfox.tollplaza.client.OsrmRoutingClient;
import com.freightfox.tollplaza.client.RoutingClient;

/**
 * Wires the external provider clients. The routing provider is chosen here (factory) from
 * {@code toll.routing.provider}, so the rest of the app only ever sees {@link RoutingClient}.
 */
@Configuration
public class HttpClientConfig {

	private static final Logger log = LoggerFactory.getLogger(HttpClientConfig.class);

	@Bean
	RestClient geocodingRestClient(TollProperties props) {
		return RestClient.builder()
				.baseUrl(props.geocoding().baseUrl())
				.defaultHeader(HttpHeaders.USER_AGENT, props.geocoding().userAgent())
				.requestFactory(requestFactory(props))
				.build();
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
