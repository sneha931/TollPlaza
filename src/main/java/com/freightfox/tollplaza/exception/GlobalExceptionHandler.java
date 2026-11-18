package com.freightfox.tollplaza.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.freightfox.tollplaza.dto.ErrorResponse;

/**
 * Translates every failure into the {@code {"error": "..."}} shape defined by the API spec.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
		// Any field-level failure (blank, wrong length, non-numeric) is an invalid pincode per the spec.
		return error(HttpStatus.BAD_REQUEST, InvalidPincodeException.MESSAGE);
	}

	@ExceptionHandler(InvalidPincodeException.class)
	ResponseEntity<ErrorResponse> handleInvalidPincode(InvalidPincodeException e) {
		return error(HttpStatus.BAD_REQUEST, e.getMessage());
	}

	@ExceptionHandler(SamePincodeException.class)
	ResponseEntity<ErrorResponse> handleSamePincode(SamePincodeException e) {
		return error(HttpStatus.BAD_REQUEST, e.getMessage());
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException e) {
		return error(HttpStatus.BAD_REQUEST, "Request body is missing or is not valid JSON");
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException e) {
		return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type must be application/json");
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException e) {
		return error(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method " + e.getMethod() + " is not supported");
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<ErrorResponse> handleUnknownPath(NoResourceFoundException e) {
		return error(HttpStatus.NOT_FOUND, "No endpoint at /" + e.getResourcePath());
	}

	@ExceptionHandler(RouteNotFoundException.class)
	ResponseEntity<ErrorResponse> handleNoRoute(RouteNotFoundException e) {
		return error(HttpStatus.NOT_FOUND, e.getMessage());
	}

	@ExceptionHandler(ExternalServiceException.class)
	ResponseEntity<ErrorResponse> handleExternal(ExternalServiceException e) {
		log.error("Upstream provider failure", e);
		return error(HttpStatus.BAD_GATEWAY, e.getMessage() + ", please try again later");
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
		log.error("Unexpected error", e);
		return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
	}

	private static ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(new ErrorResponse(message));
	}
}
