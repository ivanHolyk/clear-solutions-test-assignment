package com.holyk.clearsolutions.exceptions;

public class UserNotValidException extends RuntimeException {

	private static final long serialVersionUID = -7785964411285718029L;


	/**
	 * @param message
	 */
	public UserNotValidException(String message) {
		super(message);
	}

}
