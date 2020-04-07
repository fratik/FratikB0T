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

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.common.reflect.TypeToken;
import gg.amy.pgorm.annotations.PrimaryKey;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import pl.fratik.core.entity.DatabaseEntity;
import pl.fratik.core.event.DatabaseUpdateEvent;
import pl.fratik.core.util.GsonUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RedisCacheManager {
    private final String PREFIX;
    @Getter(AccessLevel.PACKAGE) private final JedisPool jedisPool;
    private final ExecutorService executor = Executors.newFixedThreadPool(4, new RedisThreadFactory());

    public RedisCacheManager(long id) {
        this(new JedisPool(), id);
    }

    public RedisCacheManager(JedisPool jedisPool, long id) {
        PREFIX = "FB0T-" + id + "-" + ThreadLocalRandom.current().nextInt(10);
        this.jedisPool = jedisPool;
    }

    public <T> RedisCache<T> getCache(Class<T> tak) {
        return getCache(tak, 300);
    }

    public <T> RedisCache<T> getCache(Class<T> tak, int expiry) {
        return getCache(new TypeToken<T>(tak){}, expiry);
    }

    public <T> RedisCache<T> getCache(TypeToken<T> holds) {
        return getCache(holds, 300);
    }

    public <T> RedisCache<T> getCache(TypeToken<T> holds, int expiry) {
        return new RedisCache<>(this, holds, expiry);
    }

    public <T> T get(String key, TypeToken<T> holds, Function<? super String, ? extends T> mappingFunction) {
        return get(key, holds, mappingFunction, 300);
    }

    public <T> T get(String key, TypeToken<T> holds) {
        try (Jedis jedis = jedisPool.getResource()) {
            String dbkey = PREFIX + "::" + holds.getRawType().getSimpleName() + ":" + key;
            String dane = jedis.get(dbkey);
            if (dane == null) return null;
            return GsonUtil.fromJSON(dane, holds.getType());
        }
    }

    public <T> T get(String key, TypeToken<T> holds, Function<? super String, ? extends T> mappingFunction, int expiry) {
        try (Jedis jedis = jedisPool.getResource()) {
            String dbkey = PREFIX + "::" + holds.getRawType().getSimpleName() + ":" + key;
            String dane = jedis.get(dbkey);
            if (dane == null) {
                T v = mappingFunction.apply(key);
                jedis.set(dbkey, GsonUtil.toJSON(v));
                if (expiry > 0) {
                    scheduleAsync(() -> {
                        try (Jedis j = jedisPool.getResource()) {
                            j.expire(dbkey, expiry);
                        }
                    });
                }
                return v;
            }
            return GsonUtil.fromJSON(dane, holds.getType());
        }
    }

    public <T> void putAll(TypeToken<T> holds, Map<? extends String, ? extends T> map) {
        putAll(holds, map, 300);
    }

    public <T> void putAll(TypeToken<T> holds, Map<? extends String, ? extends T> map, int expiry) {
        try (Jedis jedis = jedisPool.getResource()) {
            for (Map.Entry<? extends String, ? extends T> ent : map.entrySet()) {
                String dbkey = PREFIX + "::" + holds.getRawType().getSimpleName() + ":" + ent.getKey();
                jedis.set(dbkey, GsonUtil.toJSON(ent.getValue()));
                if (expiry > 0) {
                    scheduleAsync(() -> {
                        try (Jedis j = jedisPool.getResource()) {
                            j.expire(dbkey, expiry);
                        }
                    });
                }
            }
        }
    }

    public <T> void put(String key, TypeToken<T> holds, T value) {
        put(key, holds, value, 300);
    }

    public <T> void put(String key, TypeToken<T> holds, T value, int expiry) {
        try (Jedis jedis = jedisPool.getResource()) {
            String dbkey = PREFIX + "::" + holds.getRawType().getSimpleName() + ":" + key;
            jedis.set(dbkey, GsonUtil.toJSON(value));
            if (expiry > 0) {
                scheduleAsync(() -> {
                    try (Jedis j = jedisPool.getResource()) {
                        j.expire(dbkey, expiry);
                    }
                });
            }
        }
    }

    private void scheduleAsync(Runnable r) {
        executor.execute(r);
    }

    public <T> void invalidate(Object key, Class<T> holds) {
        invalidate(key, new TypeToken<T>(holds){});
    }

    public <T> void invalidate(Object key, TypeToken<T> holds) {
        try (Jedis jedis = jedisPool.getResource()) {
            String dbkey = PREFIX + "::" + holds.getRawType().getSimpleName() + ":" + key;
            jedis.del(dbkey);
        }
    }

    public <T> void invalidateAll(Iterable<?> keys, TypeToken<T> holds) {
        List<String> str = new ArrayList<>();
        for (Object key : keys) {
            String dbkey = PREFIX + "::" + holds.getRawType().getSimpleName() + ":" + key;
            str.add(dbkey);
        }
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(str.toArray(new String[]{}));
        }
    }

    public <T> long ttl(Object key, TypeToken<T> holds) {
        try (Jedis jedis = jedisPool.getResource()) {
            String dbkey = PREFIX + "::" + holds.getRawType().getSimpleName() + ":" + key;
            return jedis.ttl(dbkey);
        }
    }

    public <T> List<T> scanAll(TypeToken<T> holds) {
        return scanAll("*", holds);
    }

    public <T> List<T> scanAll(String pattern, TypeToken<T> holds) {
        List<String> rawResults = new ArrayList<>();
        try (Jedis jedis = jedisPool.getResource()) {
            String match = PREFIX + "::" + holds.getRawType().getSimpleName() + ":" + pattern;
            String cursor = "0";
            do {
                ScanResult<String> xd = jedis.scan(cursor, new ScanParams().match(match));
                rawResults.addAll(xd.getResult());
                cursor = xd.getStringCursor();
            } while (!cursor.equals("0"));
        }
        return rawResults.stream().<T>map(a -> GsonUtil.fromJSON(a, holds.getType())).collect(Collectors.toList());
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onDatabaseUpdate(DatabaseUpdateEvent e) {
        DatabaseEntity de = e.getEntity();
        Field field = null;
        for (Field f : de.getClass().getDeclaredFields()) {
            if (f.getAnnotation(PrimaryKey.class) == null) continue;
            field = f;
            break;
        }
        if (field == null) {
            LoggerFactory.getLogger(getClass()).warn("nie znaleziono field'a z PrimaryKey");
            return;
        }
        try {
            field.setAccessible(true);
            invalidate(field.get(de), de.getClass());
        } catch (Exception ex) {
            LoggerFactory.getLogger(getClass()).warn("wielki błąd", ex);
        }
    }

    private static class RedisThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        public RedisThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            namePrefix = "RedisCacheManager-AsyncThread-" ;
        }

        @Override
        public Thread newThread(@NotNull Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
}
