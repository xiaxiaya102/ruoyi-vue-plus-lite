package org.dromara.common.redis.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Local cache utility that keeps the original RedisUtils API surface used by this template.
 * <p>
 * This implementation is process-local and does not provide distributed cache, pub/sub, or lock semantics.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings({"unchecked", "rawtypes"})
public class RedisUtils {

    private static final long NEVER_EXPIRE = -1L;
    private static final long NOT_EXISTS = -2L;
    private static final ConcurrentHashMap<String, Entry> STORE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicLong> ATOMICS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Window> RATE_LIMITERS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, CopyOnWriteArrayList<Consumer<Object>>> SUBSCRIBERS = new ConcurrentHashMap<>();
    private static final String CLIENT_ID = "local-cache";

    public static long rateLimiter(String key, int rate, int rateInterval) {
        return rateLimiter(key, rate, rateInterval, 0);
    }

    public static long rateLimiter(String key, int rate, int rateInterval, int timeout) {
        long now = System.currentTimeMillis();
        Window window = RATE_LIMITERS.computeIfAbsent(key, k -> new Window(now, new AtomicLong(rate)));
        synchronized (window) {
            long intervalMillis = Duration.ofSeconds(rateInterval).toMillis();
            if (now - window.startedAt >= intervalMillis) {
                window.startedAt = now;
                window.permits.set(rate);
            }
            long remaining = window.permits.decrementAndGet();
            return remaining >= 0 ? remaining : -1L;
        }
    }

    public static String getClientId() {
        return CLIENT_ID;
    }

    public static <T> void publish(String channelKey, T msg, Consumer<T> consumer) {
        publish(channelKey, msg);
        consumer.accept(msg);
    }

    public static <T> void publish(String channelKey, T msg) {
        List<Consumer<Object>> consumers = SUBSCRIBERS.get(channelKey);
        if (consumers != null) {
            consumers.forEach(consumer -> consumer.accept(msg));
        }
    }

    public static <T> void subscribe(String channelKey, Class<T> clazz, Consumer<T> consumer) {
        SUBSCRIBERS.computeIfAbsent(channelKey, key -> new CopyOnWriteArrayList<>())
            .add(message -> consumer.accept(clazz.cast(message)));
    }

    public static <T> void setCacheObject(final String key, final T value) {
        STORE.put(key, Entry.never(value));
    }

    public static <T> void setCacheObject(final String key, final T value, final boolean isSaveTtl) {
        if (isSaveTtl) {
            Entry old = getLiveEntry(key);
            if (old != null) {
                STORE.put(key, new Entry(value, old.expireAt));
                return;
            }
        }
        setCacheObject(key, value);
    }

    public static <T> void setCacheObject(final String key, final T value, final Duration duration) {
        STORE.put(key, Entry.withTtl(value, duration));
    }

    public static <T> boolean setObjectIfAbsent(final String key, final T value, final Duration duration) {
        purgeExpired(key);
        Entry entry = Entry.withTtl(value, duration);
        return STORE.putIfAbsent(key, entry) == null;
    }

    public static <T> boolean setObjectIfExists(final String key, final T value, final Duration duration) {
        purgeExpired(key);
        return STORE.computeIfPresent(key, (k, old) -> Entry.withTtl(value, duration)) != null;
    }

    public static <T> void addObjectListener(final String key, final Object listener) {
        // Redis keyspace notifications are not available for local cache.
    }

    public static boolean expire(final String key, final long timeout) {
        return expire(key, Duration.ofSeconds(timeout));
    }

    public static boolean expire(final String key, final Duration duration) {
        Entry old = getLiveEntry(key);
        if (old == null) {
            return false;
        }
        STORE.put(key, Entry.withTtl(old.value, duration));
        return true;
    }

    public static <T> T getCacheObject(final String key) {
        Entry entry = getLiveEntry(key);
        return entry == null ? null : (T) entry.value;
    }

    public static <T> long getTimeToLive(final String key) {
        Entry entry = getLiveEntry(key);
        if (entry == null) {
            return NOT_EXISTS;
        }
        if (entry.expireAt == NEVER_EXPIRE) {
            return NEVER_EXPIRE;
        }
        return Math.max(0L, entry.expireAt - System.currentTimeMillis());
    }

    public static boolean deleteObject(final String key) {
        return STORE.remove(key) != null;
    }

    public static void deleteObject(final Collection collection) {
        collection.forEach(key -> STORE.remove(String.valueOf(key)));
    }

    public static boolean isExistsObject(final String key) {
        return hasKey(key);
    }

    public static <T> boolean setCacheList(final String key, final List<T> dataList) {
        STORE.put(key, Entry.never(new ArrayList<>(dataList)));
        return true;
    }

    public static <T> boolean addCacheList(final String key, final T data) {
        List<T> list = getOrCreate(key, ArrayList::new);
        return list.add(data);
    }

    public static <T> void addListListener(final String key, final Object listener) {
    }

    public static <T> List<T> getCacheList(final String key) {
        List<T> list = getCacheObject(key);
        return list == null ? new ArrayList<>() : new ArrayList<>(list);
    }

    public static <T> List<T> getCacheListRange(final String key, int form, int to) {
        List<T> list = getCacheList(key);
        if (list.isEmpty() || form >= list.size()) {
            return new ArrayList<>();
        }
        int end = Math.min(to + 1, list.size());
        return new ArrayList<>(list.subList(Math.max(0, form), end));
    }

    public static <T> boolean setCacheSet(final String key, final Set<T> dataSet) {
        STORE.put(key, Entry.never(new LinkedHashSet<>(dataSet)));
        return true;
    }

    public static <T> boolean addCacheSet(final String key, final T data) {
        Set<T> set = getOrCreate(key, LinkedHashSet::new);
        return set.add(data);
    }

    public static <T> void addSetListener(final String key, final Object listener) {
    }

    public static <T> Set<T> getCacheSet(final String key) {
        Set<T> set = getCacheObject(key);
        return set == null ? new LinkedHashSet<>() : new LinkedHashSet<>(set);
    }

    public static <T> void setCacheMap(final String key, final Map<String, T> dataMap) {
        if (dataMap != null) {
            STORE.put(key, Entry.never(new LinkedHashMap<>(dataMap)));
        }
    }

    public static <T> void addMapListener(final String key, final Object listener) {
    }

    public static <T> Map<String, T> getCacheMap(final String key) {
        Map<String, T> map = getCacheObject(key);
        return map == null ? new LinkedHashMap<>() : new LinkedHashMap<>(map);
    }

    public static <T> Set<String> getCacheMapKeySet(final String key) {
        return getCacheMap(key).keySet();
    }

    public static <T> void setCacheMapValue(final String key, final String hKey, final T value) {
        Map<String, T> map = getOrCreate(key, LinkedHashMap::new);
        map.put(hKey, value);
    }

    public static <T> T getCacheMapValue(final String key, final String hKey) {
        Map<String, T> map = getCacheObject(key);
        return map == null ? null : map.get(hKey);
    }

    public static <T> T delCacheMapValue(final String key, final String hKey) {
        Map<String, T> map = getCacheObject(key);
        return map == null ? null : map.remove(hKey);
    }

    public static <T> void delMultiCacheMapValue(final String key, final Set<String> hKeys) {
        Map<String, T> map = getCacheObject(key);
        if (map != null) {
            hKeys.forEach(map::remove);
        }
    }

    public static <K, V> Map<K, V> getMultiCacheMapValue(final String key, final Set<K> hKeys) {
        Map<K, V> map = getCacheObject(key);
        if (map == null) {
            return new LinkedHashMap<>();
        }
        return map.entrySet().stream()
            .filter(entry -> hKeys.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static void setAtomicValue(String key, long value) {
        ATOMICS.computeIfAbsent(key, k -> new AtomicLong()).set(value);
    }

    public static long getAtomicValue(String key) {
        return ATOMICS.computeIfAbsent(key, k -> new AtomicLong()).get();
    }

    public static long incrAtomicValue(String key) {
        return ATOMICS.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
    }

    public static long decrAtomicValue(String key) {
        return ATOMICS.computeIfAbsent(key, k -> new AtomicLong()).decrementAndGet();
    }

    public static Collection<String> keys(final String pattern) {
        Pattern regex = Pattern.compile(pattern.replace(".", "\\.").replace("*", ".*"));
        return STORE.keySet().stream()
            .filter(key -> getLiveEntry(key) != null)
            .filter(key -> regex.matcher(key).matches())
            .collect(Collectors.toList());
    }

    public static void deleteKeys(final String pattern) {
        keys(pattern).forEach(STORE::remove);
    }

    public static Boolean hasKey(String key) {
        return getLiveEntry(key) != null;
    }

    public static Properties localInfo() {
        Properties properties = new Properties();
        properties.setProperty("cache.type", "local-caffeine");
        properties.setProperty("cache.keys", String.valueOf(dbSize()));
        properties.setProperty("cache.clientId", CLIENT_ID);
        return properties;
    }

    public static long dbSize() {
        STORE.keySet().forEach(RedisUtils::purgeExpired);
        return STORE.size();
    }

    private static Entry getLiveEntry(String key) {
        Entry entry = STORE.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            STORE.remove(key);
            return null;
        }
        return entry;
    }

    private static void purgeExpired(String key) {
        getLiveEntry(key);
    }

    private static <T> T getOrCreate(String key, java.util.function.Supplier<T> supplier) {
        Entry entry = getLiveEntry(key);
        if (entry != null) {
            return (T) entry.value;
        }
        T value = supplier.get();
        STORE.put(key, Entry.never(value));
        return value;
    }

    private static final class Entry {
        private final Object value;
        private final long expireAt;

        private Entry(Object value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }

        private static Entry never(Object value) {
            return new Entry(value, NEVER_EXPIRE);
        }

        private static Entry withTtl(Object value, Duration duration) {
            return new Entry(value, System.currentTimeMillis() + duration.toMillis());
        }

        private boolean isExpired() {
            return expireAt != NEVER_EXPIRE && System.currentTimeMillis() >= expireAt;
        }
    }

    private static final class Window {
        private long startedAt;
        private final AtomicLong permits;

        private Window(long startedAt, AtomicLong permits) {
            this.startedAt = startedAt;
            this.permits = permits;
        }
    }
}
