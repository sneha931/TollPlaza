package com.freightfox.tollplaza.exception;

/** Both pincodes are valid but no drivable route connects them. Mapped to HTTP 404. */
public class RouteNotFoundException extends RuntimeException {

	public static final String MESSAGE = "No drivable route found between the given pincodes";

	public RouteNotFoundException() {
		super(MESSAGE);
	}
}
