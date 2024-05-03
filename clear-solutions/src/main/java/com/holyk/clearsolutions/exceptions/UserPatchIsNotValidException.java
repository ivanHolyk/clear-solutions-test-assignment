package com.holyk.clearsolutions.exceptions;

public class UserPatchIsNotValidException extends RuntimeException {

	private static final long serialVersionUID = 6863172702128132501L;

	/**
	 * @param message
	 */
	public UserPatchIsNotValidException(String message) {
		super(message);
	}

}
