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
	private static final String INVALID_JSON = """
			{"error": "Request body is missing or is not valid JSON"}""";
	private static final String NO_TOLLS = """
			{"route": {"sourcePincode": "560001", "destinationPincode": "560025", "distanceInKm": 5.24}, "tollPlazas": []}""";
	private static final String SUCCESS = """
			{
			  "route": {"sourcePincode": "560064", "destinationPincode": "411045", "distanceInKm": 848.77},
			  "tollPlazas": [
			    {"name": "Banglaore-Nelamangala Toll Plaza", "latitude": 13.09062, "longitude": 77.400019, "distanceFromSource": 25.59}
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
	@io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
			description = "Pick a scenario from the Examples dropdown (the same cases as the Postman collection).",
			content = @Content(schema = @Schema(implementation = TollPlazaRequest.class), examples = {
					@ExampleObject(name = "1. Bengaluru to Pune (assignment route)",
							summary = "200: ~850 km on NH48 with ~20 toll plazas. Send twice to see the cache.",
							value = "{\"sourcePincode\": \"560064\", \"destinationPincode\": \"411045\"}"),
					@ExampleObject(name = "2. Delhi to Bengaluru",
							summary = "200: long route, ~2,100 km with many toll plazas",
							value = "{\"sourcePincode\": \"110001\", \"destinationPincode\": \"560001\"}"),
					@ExampleObject(name = "3. Nellore to Chennai",
							summary = "200: short NH16 route, duplicate CSV plazas merged",
							value = "{\"sourcePincode\": \"524002\", \"destinationPincode\": \"600042\"}"),
					@ExampleObject(name = "4. Short city trip (no tolls)",
							summary = "200 with an empty tollPlazas array",
							value = "{\"sourcePincode\": \"560001\", \"destinationPincode\": \"560025\"}"),
					@ExampleObject(name = "5. Same source and destination",
							summary = "400: Source and destination pincodes cannot be the same",
							value = "{\"sourcePincode\": \"560064\", \"destinationPincode\": \"560064\"}"),
					@ExampleObject(name = "6. Malformed pincode",
							summary = "400: Invalid source or destination pincode (not 6 digits)",
							value = "{\"sourcePincode\": \"5600\", \"destinationPincode\": \"411045\"}"),
					@ExampleObject(name = "7. Unknown pincode",
							summary = "400: Invalid source or destination pincode (well-formed but doesn't exist)",
							value = "{\"sourcePincode\": \"999999\", \"destinationPincode\": \"411045\"}"),
					@ExampleObject(name = "8. Missing field",
							summary = "400: Invalid source or destination pincode",
							value = "{\"sourcePincode\": \"560064\"}"),
					@ExampleObject(name = "9. Not a JSON object",
							summary = "400: Request body is missing or is not valid JSON",
							value = "\"not a request object\"") }))
	@ApiResponse(responseCode = "200", description = "Route and toll plazas (empty array when the route has none)",
			content = @Content(schema = @Schema(implementation = TollPlazaResponse.class), examples = {
					@ExampleObject(name = "Toll plazas on route", value = SUCCESS),
					@ExampleObject(name = "No toll plazas", value = NO_TOLLS) }))
	@ApiResponse(responseCode = "400", description = "Malformed or unknown pincode, same source and destination, or invalid JSON",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
					@ExampleObject(name = "Invalid pincode", value = INVALID_PINCODE),
					@ExampleObject(name = "Same pincode", value = SAME_PINCODE),
					@ExampleObject(name = "Invalid JSON", value = INVALID_JSON) }))
	@ApiResponse(responseCode = "404", description = "No drivable route between the pincodes",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "502", description = "Geocoding or routing provider unavailable",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public TollPlazaResponse findTollPlazas(@Valid @RequestBody TollPlazaRequest request) {
		return service.findTollPlazas(request.sourcePincode(), request.destinationPincode());
	}
}
