package com.freightfox.tollplaza.exception;

/** A pincode is malformed or unknown to the geocoder. Mapped to HTTP 400. */
public class InvalidPincodeException extends RuntimeException {

	public static final String MESSAGE = "Invalid source or destination pincode";

	public InvalidPincodeException() {
		super(MESSAGE);
	}
}
