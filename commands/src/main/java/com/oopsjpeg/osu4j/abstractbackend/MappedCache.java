package com.oopsjpeg.osu4j.abstractbackend;

import java.util.function.Function;
import java.util.function.Supplier;

public interface MappedCache<T, R> {

	R getOrLoad(Supplier<T> supplier);

	<E extends Throwable> R getOrLoadChecked(ThrowingSupplier<T, E> supplier) throws E;

	<S> MappedCache<T, S> map(Function<R, S> mapping);
}
