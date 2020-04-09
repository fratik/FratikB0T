package com.oopsjpeg.osu4j.exception;

public class MalformedResponseException extends OsuAPIException {

	/**
	 *
	 */
	private static final long serialVersionUID = -1508365932151167272L;

	public MalformedResponseException() {
	}

	public MalformedResponseException(String message) {
		super(message);
	}

	public MalformedResponseException(Throwable cause) {
		super(cause);
	}

	public MalformedResponseException(String message, Throwable cause) {
		super(message, cause);
	}

}
