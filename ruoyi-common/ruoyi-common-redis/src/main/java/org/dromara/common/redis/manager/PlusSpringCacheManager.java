package org.dromara.common.redis.manager;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Spring Cache manager backed by local Caffeine caches.
 * <p>
 * Cache names keep the original format: name#ttl#maxIdle#maxSize#local.
 */
public class PlusSpringCacheManager implements CacheManager {

    private boolean dynamic = true;
    private boolean allowNullValues = true;
    private boolean transactionAware = true;
    private final ConcurrentMap<String, Cache> instanceMap = new ConcurrentHashMap<>();

    public void setAllowNullValues(boolean allowNullValues) {
        this.allowNullValues = allowNullValues;
    }

    public void setTransactionAware(boolean transactionAware) {
        this.transactionAware = transactionAware;
    }

    public void setCacheNames(Collection<String> names) {
        if (names != null) {
            for (String name : names) {
                getCache(name);
            }
            dynamic = false;
        } else {
            dynamic = true;
        }
    }

    @Override
    public Cache getCache(String name) {
        CacheSpec spec = CacheSpec.parse(name);
        Cache cache = instanceMap.get(spec.name());
        if (cache != null) {
            return cache;
        }
        if (!dynamic) {
            return null;
        }
        Cache created = createCache(spec);
        Cache oldCache = instanceMap.putIfAbsent(spec.name(), created);
        return oldCache == null ? created : oldCache;
    }

    private Cache createCache(CacheSpec spec) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder();
        if (spec.ttl() != null && !spec.ttl().isZero() && !spec.ttl().isNegative()) {
            builder.expireAfterWrite(spec.ttl());
        }
        if (spec.maxIdle() != null && !spec.maxIdle().isZero() && !spec.maxIdle().isNegative()) {
            builder.expireAfterAccess(spec.maxIdle());
        }
        if (spec.maxSize() > 0) {
            builder.maximumSize(spec.maxSize());
        }
        Cache cache = new LocalCaffeineCache(spec.name(), builder, allowNullValues);
        return transactionAware ? new TransactionAwareCacheDecorator(cache) : cache;
    }

    @Override
    public Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(instanceMap.keySet());
    }

    private record CacheSpec(String name, Duration ttl, Duration maxIdle, long maxSize) {
        private static CacheSpec parse(String cacheName) {
            String[] array = StringUtils.delimitedListToStringArray(cacheName, "#");
            String name = array[0];
            Duration ttl = array.length > 1 ? DurationStyle.detectAndParse(array[1]) : null;
            Duration maxIdle = array.length > 2 ? DurationStyle.detectAndParse(array[2]) : null;
            long maxSize = array.length > 3 ? Long.parseLong(array[3]) : 0;
            return new CacheSpec(name, ttl, maxIdle, maxSize);
        }
    }
}
