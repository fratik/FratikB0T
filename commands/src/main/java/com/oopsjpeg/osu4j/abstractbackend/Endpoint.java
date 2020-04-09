package com.oopsjpeg.osu4j.abstractbackend;

import com.oopsjpeg.osu4j.exception.OsuAPIException;

/**
 * Represents an endpoint of the osu-API. Providing some arguments allows you to resolve a result via an end-point.
 *
 * @param <A> the argument class
 * @param <R> the result class
 * @author WorldSEnder
 */
public interface Endpoint<A, R> {
	/**
	 * Queries the endpoint with the arguments given. Throws an {@link OsuAPIException} if anything goes wrong, such as
	 * exceeding the rate limit - although that should rarely happen if used correctly.<br>
	 * Note that this method may not instantly return, for example if the rate-limit is exceed, it may get scheduled for
	 * later execution.
	 *
	 * @param arguments the arguments to poll with
	 * @return the result returned from the API
	 * @throws OsuAPIException
	 */
	R query(A arguments) throws OsuAPIException;

	default Query<R> getAsQuery(A arguments) {
		return new Query<>(this, arguments);
	}

	default Cache<R> getCacheFor(A arguments) {
		return new SimpleItemCache<>();
	}
}
