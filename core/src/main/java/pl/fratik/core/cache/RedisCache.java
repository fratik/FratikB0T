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

import com.google.common.reflect.TypeToken;
import redis.clients.jedis.exceptions.JedisException;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class RedisCache<V> implements Cache<V> {
    private final RedisCacheManager rcm;
    private final int expiry;
    private final boolean canHandleErrors;
    private final String customName;
    private TypeToken<V> holds;

    public RedisCache(RedisCacheManager rcm, TypeToken<V> holds, int expiry, boolean canHandleErrors, String customName) {
        this.rcm = rcm;
        this.holds = holds;
        this.expiry = expiry;
        this.canHandleErrors = canHandleErrors;
        this.customName = customName;
    }

    private V get0(String key) {
        try {
            return rcm.get(key, holds, customName);
        } catch (JedisException ex) {
            if (canHandleErrors) throw ex;
            return null;
        }
    }

    private V get0(String key, Function<? super String, ? extends V> mappingFunction, int expiry) {
        try {
            return rcm.get(key, holds, customName, mappingFunction, expiry);
        } catch (JedisException ex) {
            if (canHandleErrors) throw ex;
            else return mappingFunction.apply(key);
        }
    }

    private V getRaw0(String dbkey) {
        try {
            return rcm.getRaw(dbkey, holds);
        } catch (JedisException ex) {
            if (canHandleErrors) throw ex;
            return null;
        }
    }

    @Override
    public V getIfPresent(@Nonnull Object key) {
        return get0(key.toString());
    }

    @Override
    public V get(@Nonnull String key, @Nonnull Function<? super String, ? extends V> mappingFunction) {
        return get0(key, mappingFunction, expiry);
    }

    @Override
    public Map<String, V> getAllPresent(@Nonnull Iterable<?> keys) {
        Map<String, V> map = new LinkedHashMap<>();
        for (Object obj : keys) {
            String str = obj.toString();
            V v = get0(str);
            if (v != null) map.put(str, v);
        }
        return map;
    }

    public Map<String, V> getAllPresentRaw(@Nonnull Iterable<?> keys) {
        Map<String, V> map = new LinkedHashMap<>();
        for (Object obj : keys) {
            String str = obj.toString();
            V v = getRaw0(str);
            if (v != null) map.put(str, v);
        }
        return map;
    }

    @Override
    public void put(@Nonnull String key, @Nonnull V value) {
        try {
            rcm.put(key, holds, customName, value, expiry);
        } catch (JedisException ex) {
            if (canHandleErrors) throw ex;
        }
    }

    @Override
    public void putAll(@Nonnull Map<? extends String, ? extends V> map) {
        try {
            rcm.putAll(holds, customName, map, expiry);
        } catch (JedisException ex) {
            if (canHandleErrors) throw ex;
        }
    }

    @Override
    public long getTTL(@Nonnull Object key) {
        try {
            return rcm.ttl(key, holds, customName);
        } catch (JedisException ex) {
            if (canHandleErrors) throw ex;
            return -1;
        }
    }

    @Override
    public void invalidateAll() {
        invalidateAllRaw(rcm.scanAll(holds, customName));
    }

    @Override
    public void invalidate(@Nonnull Object key) {
        try {
            rcm.invalidate(key, holds, customName);
        } catch (JedisException ex) {
            if (canHandleErrors) throw ex;
        }
    }

    @Override
    public void invalidateAll(@Nonnull Iterable<?> keys) {
        try {
            rcm.invalidateAll(keys, holds, customName);
        } catch (JedisException ex) {
            if (canHandleErrors) throw ex;
        }
    }

    private void invalidateAllRaw(@Nonnull Iterable<?> dbKeys) {
        try {
            rcm.invalidateAllRaw(dbKeys);
        } catch (JedisException ex) {
            if (canHandleErrors) throw ex;
        }
    }

    @Override
    public Map<String, V> asMap() {
        return getAllPresentRaw(rcm.scanAll(holds, customName));
    }
}
