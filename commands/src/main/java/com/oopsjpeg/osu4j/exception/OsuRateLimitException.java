package com.oopsjpeg.osu4j.exception;

public class OsuRateLimitException extends OsuAPIException {
	private static final long serialVersionUID = 2007081453956527716L;

	private final int requestsPerMinute;

	public OsuRateLimitException(int requestsPerMinute) {
		super("You have hit the rate limit for requests: " + requestsPerMinute + "/m");
		this.requestsPerMinute = requestsPerMinute;
	}

	public int getRequestsPerMinute() {
		return requestsPerMinute;
	}
}
