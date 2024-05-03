package com.holyk.clearsolutions.exceptions;

public class DateRangeIsNotValidException extends RuntimeException {

	private static final long serialVersionUID = 8166465100800236021L;

	
	/**
	 * @param message
	 */
	public DateRangeIsNotValidException(String message) {
		super(message);
	}

}
