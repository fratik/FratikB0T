/*
 * Copyright (C) 2019-2021 FratikB0T Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package pl.fratik.core.util;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class ExpiringHashMap<K, V> extends HashMap<K, V> {
    private static final ScheduledExecutorService executor;

    private final long delay;
    private final TimeUnit unit;

    static {
        executor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("ExpiringHashMap"));
        Runtime.getRuntime().addShutdownHook(new Thread(executor::shutdown));
    }

    public ExpiringHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
        delay = -1;
        unit = null;
    }

    public ExpiringHashMap(int initialCapacity) {
        super(initialCapacity);
        delay = -1;
        unit = null;
    }

    public ExpiringHashMap() {
        super();
        delay = -1;
        unit = null;
    }

    public ExpiringHashMap(Map<? extends K, ? extends V> m) {
        super(m);
        delay = -1;
        unit = null;
    }

    public ExpiringHashMap(int initialCapacity, float loadFactor, long delay, TimeUnit unit) {
        super(initialCapacity, loadFactor);
        this.delay = delay;
        this.unit = unit;
    }

    public ExpiringHashMap(int initialCapacity, long delay, TimeUnit unit) {
        super(initialCapacity);
        this.delay = delay;
        this.unit = unit;
    }

    public ExpiringHashMap(long delay, TimeUnit unit) {
        super();
        this.delay = delay;
        this.unit = unit;
    }

    public ExpiringHashMap(Map<? extends K, ? extends V> m, long delay, TimeUnit unit) {
        super(m);
        this.delay = delay;
        this.unit = unit;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        putAll(m, delay, unit);
    }

    public void putAll(Map<? extends K, ? extends V> m, long delay, TimeUnit unit) {
        putAll(m);
        m.forEach((k, v) -> scheduleRemoval(k, v, delay, unit));
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return replace(key, oldValue, newValue, delay, unit);
    }

    public boolean replace(K key, V oldValue, V newValue, long delay, TimeUnit unit) {
        boolean replaced = super.replace(key, oldValue, newValue);
        if (replaced) scheduleRemoval(key, newValue, delay, unit);
        return replaced;
    }

    @Override
    public V replace(K key, V value) {
        return replace(key, value, delay, unit);
    }

    public V replace(K key, V value, long delay, TimeUnit unit) {
        V v = super.replace(key, value);
        scheduleRemoval(key, value, delay, unit);
        return v;
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return computeIfAbsent(key, mappingFunction, delay, unit);
    }

    public V computeIfAbsent(K key, Function<? super K,? extends V> mappingFunction, long delay, TimeUnit unit) {
        boolean containsKey = super.containsKey(key);
        if (containsKey) return super.get(key);
        V v = mappingFunction.apply(key);
        put(key, v, delay, unit);
        return v;
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (remappingFunction == null) throw new NullPointerException();
        return computeIfPresent(key, remappingFunction, delay, unit);
    }

    public V computeIfPresent(K key, BiFunction<? super K,? super V,? extends V> remappingFunction, long delay, TimeUnit unit) {
        if (remappingFunction == null) throw new NullPointerException();
        V old = super.get(key);
        if (old == null) return null;
        V v = remappingFunction.apply(key, old);
        if (!replace(key, old, v, delay, unit)) throw new ConcurrentModificationException();
        return v;
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return compute(key, remappingFunction, delay, unit);
    }

    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction, long delay, TimeUnit unit) {
        if (remappingFunction == null) throw new NullPointerException();
        V old = super.get(key);
        V v = remappingFunction.apply(key, old);
        if (old != null) {
            if (!replace(key, old, v, delay, unit)) throw new ConcurrentModificationException();
        } else {
            put(key, v, delay, unit);
        }
        return v;
    }

    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return merge(key, value, remappingFunction, delay, unit);
    }

    public V merge(K key, V value, BiFunction<? super V,? super V,? extends V> remappingFunction, long delay, TimeUnit unit) {
        if (value == null) throw new NullPointerException();
        if (remappingFunction == null) throw new NullPointerException();
        V v;
        V oldValue = super.get(key);
        if (oldValue != null) {
            v = remappingFunction.apply(oldValue, value);
            if (!replace(key, oldValue, v, delay, unit)) throw new ConcurrentModificationException();
        } else {
            v = value;
            put(key, v, delay, unit);
        }
        return v;
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        replaceAll(function, delay, unit);
    }

    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function, long delay, TimeUnit unit) {
        for (Map.Entry<K, V> entry : this.entrySet())
            entry.setValue(function.apply(entry.getKey(), entry.getValue()));
    }

    @Override
    public V put(K key, V value) {
        return put(key, value, delay, unit);
    }

    public V put(K key, V value, long delay, TimeUnit unit) {
        V v = super.put(key, value);
        scheduleRemoval(key, value, delay, unit);
        return v;
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return putIfAbsent(key, value, delay, unit);
    }

    public V putIfAbsent(K key, V value, long delay, TimeUnit unit) {
        V v = super.putIfAbsent(key, value);
        scheduleRemoval(key, value, delay, unit);
        return v;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ExpiringHashMap<?, ?> that = (ExpiringHashMap<?, ?>) o;
        return delay == that.delay && unit == that.unit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), delay, unit);
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return new ExpiringEntrySet();
    }

    private void scheduleRemoval(Object key, Object value, long delay, TimeUnit unit) {
        if (delay != -1 && unit != null) executor.schedule(() -> remove(key, value), delay, unit);
    }

    class ExpiringIterator implements Iterator<Entry<K, V>> {
        private final Iterator<Entry<K, V>> iterator;

        public ExpiringIterator(Iterator<Entry<K, V>> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Entry<K, V> next() {
            Entry<K, V> next = iterator.next();
            return new ExpiringEntry(next);
        }
    }

    class ExpiringEntrySet extends AbstractSet<Entry<K, V>> {
        @Override
        public Iterator<Entry<K, V>> iterator() {
            return new ExpiringIterator(ExpiringHashMap.super.entrySet().iterator());
        }

        @Override
        public int size() {
            return ExpiringHashMap.super.entrySet().size();
        }

        @Override
        public void clear() {
            ExpiringHashMap.this.clear();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> e = (Map.Entry<?,?>) o;
            Object key = e.getKey();
            V expected = ExpiringHashMap.this.get(key);
            return expected != null && expected.equals(e.getValue());
        }

        @Override
        public void forEach(Consumer<? super Entry<K, V>> action) {
            ExpiringHashMap.this.entrySet().forEach(action);
        }
    }

    class ExpiringEntry implements Entry<K, V> {
        private K key;
        private V value;

        public ExpiringEntry(Entry<K, V> next) {
            key = next.getKey();
            value = next.getValue();
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            return replace(key, value);
        }
    }
}

