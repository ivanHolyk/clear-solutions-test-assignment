package com.holyk.clearsolutions.exceptions;

public class UserAgeNotSatisfyException extends RuntimeException {

	private static final long serialVersionUID = -7785964411285718029L;

	/**
	 * @param message
	 */
	public UserAgeNotSatisfyException(String message) {
		super(message);
	}

}
