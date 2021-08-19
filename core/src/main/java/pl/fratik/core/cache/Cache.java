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

package pl.fratik.core.cache;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.function.Function;

public interface Cache<V> {
    V getIfPresent(@Nonnull Object key);
    V get(@Nonnull String key, @Nonnull Function<String, ? extends V> mappingFunction);
    Map<String, V> getAllPresent(@Nonnull Iterable<?> keys);
    void put(@Nonnull String key, @Nonnull V value);
    void putAll(@Nonnull Map<String, ? extends V> map);
    void invalidate(@Nonnull Object key);
    long getTTL(@Nonnull Object key);
    void invalidateAll();
    void invalidateAll(@Nonnull Iterable<?> keys);
    Map<String, V> asMap();
}
