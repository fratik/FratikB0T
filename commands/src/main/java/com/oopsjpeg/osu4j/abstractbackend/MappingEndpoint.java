package com.oopsjpeg.osu4j.abstractbackend;

import com.oopsjpeg.osu4j.exception.OsuAPIException;

import java.util.Objects;
import java.util.function.Function;

public class MappingEndpoint<A, R> implements Endpoint<A, R> {
	private EndpointProxy<A, R, ?, ?> proxy;

	public <B, S> MappingEndpoint(Endpoint<B, S> actualEndpoint, Function<A, B> argumentMap, Function<S, R> resultMap) {
		this.proxy = new EndpointProxy<>(actualEndpoint, argumentMap, resultMap);
	}

	@Override
	public R query(A arguments) throws OsuAPIException {
		return proxy.query(arguments);
	}

	private static class EndpointProxy<A, R, B, S> {
		private Endpoint<B, S> actualBackend;
		private Function<A, B> argumentMapper;
		private Function<S, R> resultMapper;

		public EndpointProxy(Endpoint<B, S> actualBackend, Function<A, B> argumentMap, Function<S, R> resultMap) {
			this.actualBackend = Objects.requireNonNull(actualBackend);
			this.argumentMapper = Objects.requireNonNull(argumentMap);
			this.resultMapper = Objects.requireNonNull(resultMap);
		}

		public R query(A arguments) throws OsuAPIException {
			return resultMapper.apply(actualBackend.query(argumentMapper.apply(arguments)));
		}
	}

}
