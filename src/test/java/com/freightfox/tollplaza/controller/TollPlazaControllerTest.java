package com.freightfox.tollplaza.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.freightfox.tollplaza.dto.TollPlazaResponse;
import com.freightfox.tollplaza.dto.TollPlazaResponse.RouteInfo;
import com.freightfox.tollplaza.dto.TollPlazaResponse.TollPlazaInfo;
import com.freightfox.tollplaza.exception.ExternalServiceException;
import com.freightfox.tollplaza.exception.InvalidPincodeException;
import com.freightfox.tollplaza.exception.RouteNotFoundException;
import com.freightfox.tollplaza.exception.SamePincodeException;
import com.freightfox.tollplaza.service.TollPlazaService;

@WebMvcTest(TollPlazaController.class)
class TollPlazaControllerTest {

	private static final String URL = "/api/v1/toll-plazas";

	@Autowired
	MockMvc mvc;

	@MockitoBean
	TollPlazaService service;

	@Test
	void returnsRouteAndTollPlazas() throws Exception {
		when(service.findTollPlazas("560064", "411045")).thenReturn(new TollPlazaResponse(
				new RouteInfo("560064", "411045", 840.5),
				List.of(new TollPlazaInfo("Nelamangala Toll Plaza", 13.09, 77.40, 25.3))));

		mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("560064", "411045")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.route.sourcePincode").value("560064"))
				.andExpect(jsonPath("$.route.destinationPincode").value("411045"))
				.andExpect(jsonPath("$.route.distanceInKm").value(840.5))
				.andExpect(jsonPath("$.tollPlazas[0].name").value("Nelamangala Toll Plaza"))
				.andExpect(jsonPath("$.tollPlazas[0].latitude").value(13.09))
				.andExpect(jsonPath("$.tollPlazas[0].longitude").value(77.40))
				.andExpect(jsonPath("$.tollPlazas[0].distanceFromSource").value(25.3));
	}

	@Test
	void returnsEmptyArrayWhenNoTollPlazas() throws Exception {
		when(service.findTollPlazas("110001", "110002"))
				.thenReturn(new TollPlazaResponse(new RouteInfo("110001", "110002", 3.2), List.of()));

		mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("110001", "110002")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tollPlazas").isEmpty());
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "12345", "1234567", "012345", "56A064", "  560064" })
	void rejectsMalformedPincodeBeforeCallingService(String bad) throws Exception {
		mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body(bad, "411045")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Invalid source or destination pincode"));
		verifyNoInteractions(service);
	}

	@Test
	void rejectsMissingField() throws Exception {
		mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"sourcePincode\":\"560064\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Invalid source or destination pincode"));
	}

	@Test
	void rejectsMalformedJson() throws Exception {
		mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{not json"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Request body is missing or is not valid JSON"));
	}

	@Test
	void rejectsNonJsonContentType() throws Exception {
		mvc.perform(post(URL).contentType(MediaType.TEXT_PLAIN).content("560064"))
				.andExpect(status().isUnsupportedMediaType());
	}

	@Test
	void rejectsGet() throws Exception {
		mvc.perform(get(URL)).andExpect(status().isMethodNotAllowed());
	}

	@Test
	void unknownPathIs404NotServerError() throws Exception {
		mvc.perform(get("/no-such-endpoint"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("No endpoint at /no-such-endpoint"));
	}

	@Test
	void mapsSamePincodeTo400() throws Exception {
		when(service.findTollPlazas(anyString(), anyString())).thenThrow(new SamePincodeException());

		mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("560064", "560064")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Source and destination pincodes cannot be the same"));
	}

	@Test
	void mapsUnknownPincodeTo400() throws Exception {
		when(service.findTollPlazas(anyString(), anyString())).thenThrow(new InvalidPincodeException());

		mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("999999", "411045")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Invalid source or destination pincode"));
	}

	@Test
	void mapsNoRouteTo404() throws Exception {
		when(service.findTollPlazas(anyString(), anyString())).thenThrow(new RouteNotFoundException());

		mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("560064", "744101")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("No drivable route found between the given pincodes"));
	}

	@Test
	void mapsUpstreamFailureTo502() throws Exception {
		when(service.findTollPlazas(anyString(), anyString()))
				.thenThrow(new ExternalServiceException("Routing service is unavailable", new RuntimeException()));

		mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("560064", "411045")))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.error").value("Routing service is unavailable, please try again later"));
	}

	private static String body(String source, String destination) {
		return "{\"sourcePincode\":\"" + source + "\",\"destinationPincode\":\"" + destination + "\"}";
	}
}
