package com.freightfox.tollplaza.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.freightfox.tollplaza.dto.ErrorResponse;
import com.freightfox.tollplaza.dto.TollPlazaRequest;
import com.freightfox.tollplaza.dto.TollPlazaResponse;
import com.freightfox.tollplaza.service.TollPlazaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Toll plazas")
@RestController
@RequestMapping("/api/v1/toll-plazas")
public class TollPlazaController {

	private static final String INVALID_PINCODE = """
			{"error": "Invalid source or destination pincode"}""";
	private static final String SAME_PINCODE = """
			{"error": "Source and destination pincodes cannot be the same"}""";
	private static final String SUCCESS = """
			{
			  "route": {"sourcePincode": "560064", "destinationPincode": "411045", "distanceInKm": 851.64},
			  "tollPlazas": [
			    {"name": "Kadathanamale Toll Plaza", "latitude": 13.2179, "longitude": 77.5531, "distanceFromSource": 9.42},
			    {"name": "Hulikunte Toll Plaza", "latitude": 13.4815, "longitude": 77.4256, "distanceFromSource": 41.97}
			  ]
			}""";

	private final TollPlazaService service;

	public TollPlazaController(TollPlazaService service) {
		this.service = service;
	}

	@Operation(summary = "Find toll plazas between two pincodes",
			description = "Geocodes both pincodes, gets the fastest driving route and returns every toll plaza "
					+ "within 500 m of it, sorted by distance from the source (km). "
					+ "Returns an empty tollPlazas array when the route has none.")
	@ApiResponse(responseCode = "200", description = "Route and toll plazas",
			content = @Content(schema = @Schema(implementation = TollPlazaResponse.class),
					examples = @ExampleObject(value = SUCCESS)))
	@ApiResponse(responseCode = "400", description = "Malformed or unknown pincode, same source and destination, or invalid JSON",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
					@ExampleObject(name = "Invalid pincode", value = INVALID_PINCODE),
					@ExampleObject(name = "Same pincode", value = SAME_PINCODE) }))
	@ApiResponse(responseCode = "404", description = "No drivable route between the pincodes",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "502", description = "Geocoding or routing provider unavailable",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public TollPlazaResponse findTollPlazas(@Valid @RequestBody TollPlazaRequest request) {
		return service.findTollPlazas(request.sourcePincode(), request.destinationPincode());
	}
}
