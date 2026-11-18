package com.freightfox.tollplaza.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI tollPlazaOpenApi() {
		return new OpenAPI().info(new Info()
				.title("Toll Plaza Service")
				.version("v1")
				.description("""
						Returns the toll plazas on the driving route between two Indian pincodes, \
						sorted by distance from the source. Routes come from Mappls (OSRM fallback), \
						pincodes are geocoded with OpenStreetMap Nominatim, and toll plazas come from \
						the provided toll_plaza_india.csv dataset. Results are cached per pincode pair."""));
	}
}
