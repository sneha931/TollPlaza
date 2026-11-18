package com.freightfox.tollplaza.exception;

/** An upstream provider (geocoding / routing) failed or timed out. Mapped to HTTP 502. */
public class ExternalServiceException extends RuntimeException {

	public ExternalServiceException(String message) {
		super(message);
	}

	public ExternalServiceException(String message, Throwable cause) {
		super(message, cause);
	}
}
