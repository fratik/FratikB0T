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
import java.util.concurrent.TimeUnit;

public class ExpiringHashSet<E> extends AbstractSet<E> {
    private static final Object PRESENT = new Object();

    private final ExpiringHashMap<E, Object> map;
    private final long delay;
    private final TimeUnit unit;

    public ExpiringHashSet() {
        map = new ExpiringHashMap<>();
        delay = -1;
        unit = null;
    }

    public ExpiringHashSet(Collection<? extends E> c) {
        map = new ExpiringHashMap<>(Math.max((int) (c.size()/.75f) + 1, 16));
        delay = -1;
        unit = null;
        addAll(c);
    }

    public ExpiringHashSet(int initialCapacity, float loadFactor) {
        map = new ExpiringHashMap<>(initialCapacity, loadFactor);
        delay = -1;
        unit = null;
    }

    public ExpiringHashSet(int initialCapacity) {
        map = new ExpiringHashMap<>(initialCapacity);
        delay = -1;
        unit = null;
    }

    public ExpiringHashSet(long delay, TimeUnit unit) {
        map = new ExpiringHashMap<>(delay, unit);
        this.delay = delay;
        this.unit = unit;
    }

    public ExpiringHashSet(Collection<? extends E> c, long delay, TimeUnit unit) {
        map = new ExpiringHashMap<>(Math.max((int) (c.size()/.75f) + 1, 16), delay, unit);
        this.delay = delay;
        this.unit = unit;
        addAll(c);
    }

    public ExpiringHashSet(int initialCapacity, float loadFactor, long delay, TimeUnit unit) {
        map = new ExpiringHashMap<>(initialCapacity, loadFactor, delay, unit);
        this.delay = delay;
        this.unit = unit;
    }

    public ExpiringHashSet(int initialCapacity, long delay, TimeUnit unit) {
        map = new ExpiringHashMap<>(initialCapacity, delay, unit);
        this.delay = delay;
        this.unit = unit;
    }

    @Override
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    public boolean add(E e) {
        return add(e, delay, unit);
    }

    public boolean add(E e, long delay, TimeUnit unit) {
        return map.put(e, PRESENT, delay, unit) == null;
    }

    @Override
    public boolean remove(Object o) {
        return map.remove(o) == PRESENT;
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ExpiringHashSet<?> that = (ExpiringHashSet<?>) o;
        return Objects.equals(map, that.map);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), map);
    }
}
