package com.holyk.clearsolutions.exceptions;

public class UserNotFoundException extends RuntimeException {

	private static final long serialVersionUID = -8991897541908359023L;

	/**
	 * @param message
	 */
	public UserNotFoundException(String message) {
		super(message);
	}

}
