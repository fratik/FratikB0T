package com.oopsjpeg.osu4j.abstractbackend;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class SimpleItemCache<T> implements Cache<T> {
	private final Object resultLock = this;
	private volatile T cached;
	private volatile boolean isCached;
	private List<SimpleMappedItemCache<?>> children = new ArrayList<>();

	public SimpleItemCache() {
	}

	private void onNewResult() {
		assert Thread.holdsLock(resultLock);
		for (SimpleMappedItemCache<?> child : children) {
			child.triggerNewResult(cached);
		}
	}

	@Override
	public T getOrLoad(Supplier<T> supplier) {
		Objects.requireNonNull(supplier);
		if (isCached) {
			return cached;
		}
		synchronized (resultLock) {
			this.cached = supplier.get();
			onNewResult();
			this.isCached = true;
			return this.cached;
		}
	}

	@Override
	public <E extends Throwable> T getOrLoadChecked(ThrowingSupplier<T, E> supplier) throws E {
		Objects.requireNonNull(supplier);
		if (isCached) {
			return cached;
		}
		synchronized (resultLock) {
			this.cached = supplier.get(); // Throws
			onNewResult();
			this.isCached = true;
			return this.cached;
		}
	}

	@Override
	public <S> MappedCache<T, S> map(Function<T, S> mapping) {
		synchronized (resultLock) {
			SimpleMappedItemCache<S> newChild = new SimpleMappedItemCache<>(mapping);
			if (isCached) {
				newChild.triggerNewResult(cached);
			}
			children.add(newChild);
			return newChild;
		}
	}

	private class SimpleMappedItemCache<R> implements MappedCache<T, R> {
		private R result;
		private Function<T, R> remapping;

		public SimpleMappedItemCache(Function<T, R> remapping) {
			this.remapping = Objects.requireNonNull(remapping);
		}

		protected void triggerNewResult(T newResult) {
			this.result = remapping.apply(newResult);
		}

		@Override
		public R getOrLoad(Supplier<T> supplier) {
			SimpleItemCache.this.getOrLoad(supplier);
			return result;
		}

		@Override
		public <E extends Throwable> R getOrLoadChecked(ThrowingSupplier<T, E> supplier) throws E {
			SimpleItemCache.this.getOrLoadChecked(supplier);
			return result;
		}

		@Override
		public <S> MappedCache<T, S> map(Function<R, S> mapping) {
			return SimpleItemCache.this.map(remapping.andThen(mapping));
		}
	}

}
