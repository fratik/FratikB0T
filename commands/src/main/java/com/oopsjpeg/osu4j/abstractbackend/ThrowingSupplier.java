package com.oopsjpeg.osu4j.abstractbackend;

@FunctionalInterface
public interface ThrowingSupplier<T, E extends Throwable> {

	T get() throws E;

}
