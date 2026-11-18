package com.freightfox.tollplaza.exception;

/** Source and destination pincodes are identical. Mapped to HTTP 400. */
public class SamePincodeException extends RuntimeException {

	public static final String MESSAGE = "Source and destination pincodes cannot be the same";

	public SamePincodeException() {
		super(MESSAGE);
	}
}
