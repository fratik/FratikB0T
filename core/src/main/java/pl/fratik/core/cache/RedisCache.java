/*
 * Copyright (C) 2020 FratikB0T Contributors
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

import com.google.common.reflect.TypeToken;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class RedisCache<V> implements Cache<V> {
    private final RedisCacheManager rcm;
    private final TypeToken<V> holds;
    private final int expiry;

    public RedisCache(RedisCacheManager rcm, TypeToken<V> holds, int expiry) {
        this.rcm = rcm;
        this.holds = holds;
        this.expiry = expiry;
    }

    @Override
    public V getIfPresent(@Nonnull Object key) {
        return rcm.get(key.toString(), holds);
    }

    @Override
    public V get(@Nonnull String key, @Nonnull Function<? super String, ? extends V> mappingFunction) {
        return rcm.get(key, holds, mappingFunction, expiry);
    }

    @Override
    public Map<String, V> getAllPresent(@Nonnull Iterable<?> keys) {
        Map<String, V> map = new LinkedHashMap<>();
        for (Object obj : keys) {
            String str = obj.toString();
            V v = rcm.get(str, holds);
            if (v != null) map.put(str, v);
        }
        return map;
    }

    @Override
    public void put(@Nonnull String key, @Nonnull V value) {
        rcm.put(key, holds, value, expiry);
    }

    @Override
    public void putAll(@Nonnull Map<? extends String, ? extends V> map) {
        rcm.putAll(holds, map, expiry);
    }

    @Override
    public long getTTL(@Nonnull Object key) {
        return rcm.ttl(key, holds);
    }

    @Override
    public void invalidateAll() {
        invalidateAll(rcm.scanAll(holds));
    }

    @Override
    public void invalidate(@Nonnull Object key) {
        rcm.invalidate(key, holds);
    }

    @Override
    public void invalidateAll(@Nonnull Iterable<?> keys) {
        rcm.invalidateAll(keys, holds);
    }

    @Override
    public Map<String, V> asMap() {
        return getAllPresent(rcm.scanAll(holds));
    }
}
