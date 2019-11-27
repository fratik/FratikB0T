package com.oopsjpeg.osu4j.exception;

import java.io.IOException;

public class OsuAPIException extends IOException {

	/**
	 *
	 */
	private static final long serialVersionUID = 1100532333860547919L;

	public OsuAPIException() {
	}

	public OsuAPIException(String message) {
		super(message);
	}

	public OsuAPIException(Throwable cause) {
		super(cause);
	}

	public OsuAPIException(String message, Throwable cause) {
		super(message, cause);
	}

}
