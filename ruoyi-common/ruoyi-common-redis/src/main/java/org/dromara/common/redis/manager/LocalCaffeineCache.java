package org.dromara.common.redis.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.support.NullValue;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring Cache adapter backed by a local Caffeine cache.
 */
public class LocalCaffeineCache implements org.springframework.cache.Cache {

    private final String name;
    private final Cache<Object, Object> cache;
    private final ConcurrentHashMap<Object, Object> keyIndex = new ConcurrentHashMap<>();
    private final boolean allowNullValues;

    public LocalCaffeineCache(String name, Caffeine<Object, Object> builder, boolean allowNullValues) {
        this.name = name;
        this.allowNullValues = allowNullValues;
        this.cache = builder
            .removalListener((key, value, cause) -> keyIndex.remove(key))
            .build();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return cache;
    }

    @Override
    public ValueWrapper get(Object key) {
        Object value = cache.getIfPresent(key);
        return value == null ? null : () -> fromStoreValue(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Class<T> type) {
        Object value = fromStoreValue(cache.getIfPresent(key));
        if (value == null) {
            return null;
        }
        if (type != null && !type.isInstance(value)) {
            throw new IllegalStateException("Cached value is not of required type [" + type.getName() + "]");
        }
        return (T) value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Callable<T> valueLoader) {
        Object value = cache.get(key, k -> {
            try {
                T loaded = valueLoader.call();
                keyIndex.put(k, Boolean.TRUE);
                return toStoreValue(loaded);
            } catch (Exception e) {
                throw new ValueRetrievalException(key, valueLoader, e);
            }
        });
        return (T) fromStoreValue(value);
    }

    @Override
    public void put(Object key, Object value) {
        cache.put(key, toStoreValue(value));
        keyIndex.put(key, Boolean.TRUE);
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        Object existing = cache.asMap().putIfAbsent(key, toStoreValue(value));
        if (existing == null) {
            keyIndex.put(key, Boolean.TRUE);
            return null;
        }
        return () -> fromStoreValue(existing);
    }

    @Override
    public void evict(Object key) {
        evictIfPresent(key);
    }

    @Override
    public boolean evictIfPresent(Object key) {
        boolean existed = cache.asMap().remove(key) != null;
        keyIndex.remove(key);
        return existed;
    }

    @Override
    public void clear() {
        cache.invalidateAll();
        keyIndex.clear();
    }

    @Override
    public boolean invalidate() {
        boolean hasEntries = !keyIndex.isEmpty();
        clear();
        return hasEntries;
    }

    private Object toStoreValue(Object value) {
        if (value == null) {
            if (!allowNullValues) {
                throw new IllegalArgumentException("Cache '" + name + "' does not allow null values");
            }
            return NullValue.INSTANCE;
        }
        return value;
    }

    private Object fromStoreValue(Object value) {
        return value == NullValue.INSTANCE ? null : value;
    }
}
