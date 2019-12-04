package com.oopsjpeg.osu4j.abstractbackend;

import com.oopsjpeg.osu4j.exception.OsuAPIException;

import java.util.Objects;

/**
 * Represents an endpoint where arguments are already given. Abstracts out the argument class.
 *
 * @param <R> the result type.
 * @author WorldSEnder
 */
public final class Query<R> {
	private final EndpointWithArguments<?, R> filledEndpoint;

	public <A> Query(Endpoint<A, R> endpoint, A arguments) {
		filledEndpoint = new EndpointWithArguments<>(endpoint, arguments);
	}

	public R resolve() throws OsuAPIException {
		return filledEndpoint.resolve();
	}

	public LazilyLoaded<R> asLazilyLoaded() {
		return getLazyResult(filledEndpoint.getDefaultCache());
	}

	public LazilyLoaded<R> getLazyResult(Cache<R> cacheToUse) {
		return new LazilyLoaded<>(this, cacheToUse);
	}

	@Override
	public String toString() {
		return "Query[" + filledEndpoint.toString() + "]";
	}

	private static class EndpointWithArguments<A, R> {
		private final Endpoint<A, R> endpoint;
		private final A arguments;

		public EndpointWithArguments(Endpoint<A, R> endPoint, A arguments) {
			this.endpoint = Objects.requireNonNull(endPoint);
			this.arguments = Objects.requireNonNull(arguments);
		}

		public R resolve() throws OsuAPIException {
			return endpoint.query(arguments);
		}

		@Override
		public String toString() {
			return endpoint.toString() + "(" + arguments.toString() + ")";
		}

		public Cache<R> getDefaultCache() {
			return endpoint.getCacheFor(arguments);
		}
	}
}
