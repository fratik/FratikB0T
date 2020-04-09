package com.oopsjpeg.osu4j.exception;

public class MalformedRequestException extends OsuAPIException {

	/**
	 *
	 */
	private static final long serialVersionUID = 597460123884347499L;

	public MalformedRequestException() {
	}

	public MalformedRequestException(String message) {
		super(message);
	}

	public MalformedRequestException(Throwable cause) {
		super(cause);
	}

	public MalformedRequestException(String message, Throwable cause) {
		super(message, cause);
	}

}
