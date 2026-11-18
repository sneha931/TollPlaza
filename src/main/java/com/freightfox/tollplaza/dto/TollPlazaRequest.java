package com.freightfox.tollplaza.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Indian pincodes are exactly six digits and never start with 0.
 */
public record TollPlazaRequest(
		@Schema(description = "6-digit Indian pincode of the start point", example = "560064", pattern = PINCODE_REGEX)
		@NotBlank @Pattern(regexp = PINCODE_REGEX) String sourcePincode,
		@Schema(description = "6-digit Indian pincode of the end point", example = "411045", pattern = PINCODE_REGEX)
		@NotBlank @Pattern(regexp = PINCODE_REGEX) String destinationPincode) {

	public static final String PINCODE_REGEX = "^[1-9][0-9]{5}$";
}
