package org.dromara.common.redis.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Process-local queue utility.
 */
@Deprecated
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class QueueUtils {

    private static final ConcurrentHashMap<String, LinkedBlockingQueue<Object>> QUEUES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, DelayQueue<DelayedValue<Object>>> DELAYED_QUEUES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, PriorityBlockingQueue<Object>> PRIORITY_QUEUES = new ConcurrentHashMap<>();

    public static <T> boolean addQueueObject(String queueName, T data) {
        return QUEUES.computeIfAbsent(queueName, key -> new LinkedBlockingQueue<>()).offer(data);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getQueueObject(String queueName) {
        return (T) QUEUES.computeIfAbsent(queueName, key -> new LinkedBlockingQueue<>()).poll();
    }

    public static <T> boolean removeQueueObject(String queueName, T data) {
        return QUEUES.computeIfAbsent(queueName, key -> new LinkedBlockingQueue<>()).remove(data);
    }

    public static <T> boolean destroyQueue(String queueName) {
        return QUEUES.remove(queueName) != null;
    }

    public static <T> void addDelayedQueueObject(String queueName, T data, long time) {
        addDelayedQueueObject(queueName, data, time, TimeUnit.MILLISECONDS);
    }

    public static <T> void addDelayedQueueObject(String queueName, T data, long time, TimeUnit timeUnit) {
        DELAYED_QUEUES.computeIfAbsent(queueName, key -> new DelayQueue<>())
            .offer(new DelayedValue<>(data, timeUnit.toMillis(time)));
    }

    @SuppressWarnings("unchecked")
    public static <T> T getDelayedQueueObject(String queueName) {
        DelayedValue<Object> value = DELAYED_QUEUES.computeIfAbsent(queueName, key -> new DelayQueue<>()).poll();
        return value == null ? null : (T) value.value();
    }

    public static <T> boolean removeDelayedQueueObject(String queueName, T data) {
        return DELAYED_QUEUES.computeIfAbsent(queueName, key -> new DelayQueue<>())
            .removeIf(value -> value.value().equals(data));
    }

    public static <T> void destroyDelayedQueue(String queueName) {
        DELAYED_QUEUES.remove(queueName);
    }

    public static <T> boolean addPriorityQueueObject(String queueName, T data) {
        return PRIORITY_QUEUES.computeIfAbsent(queueName, key -> new PriorityBlockingQueue<>(11, comparator()))
            .offer(data);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getPriorityQueueObject(String queueName) {
        return (T) PRIORITY_QUEUES.computeIfAbsent(queueName, key -> new PriorityBlockingQueue<>(11, comparator())).poll();
    }

    public static <T> boolean removePriorityQueueObject(String queueName, T data) {
        return PRIORITY_QUEUES.computeIfAbsent(queueName, key -> new PriorityBlockingQueue<>(11, comparator())).remove(data);
    }

    public static <T> boolean destroyPriorityQueue(String queueName) {
        return PRIORITY_QUEUES.remove(queueName) != null;
    }

    public static <T> boolean trySetBoundedQueueCapacity(String queueName, int capacity) {
        QUEUES.computeIfAbsent(queueName, key -> new LinkedBlockingQueue<>(capacity));
        return true;
    }

    public static <T> boolean trySetBoundedQueueCapacity(String queueName, int capacity, boolean destroy) {
        if (destroy) {
            QUEUES.remove(queueName);
        }
        return trySetBoundedQueueCapacity(queueName, capacity);
    }

    public static <T> boolean addBoundedQueueObject(String queueName, T data) {
        return addQueueObject(queueName, data);
    }

    public static <T> T getBoundedQueueObject(String queueName) {
        return getQueueObject(queueName);
    }

    public static <T> boolean removeBoundedQueueObject(String queueName, T data) {
        return removeQueueObject(queueName, data);
    }

    public static <T> boolean destroyBoundedQueue(String queueName) {
        return destroyQueue(queueName);
    }

    public static <T> void subscribeBlockingQueue(String queueName, Function<T, CompletionStage<Void>> consumer, boolean isDelayed) {
        CompletableFuture.runAsync(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                T data = isDelayed ? getDelayedQueueObject(queueName) : getQueueObject(queueName);
                if (data != null) {
                    consumer.apply(data);
                }
            }
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Comparator<Object> comparator() {
        return (left, right) -> {
            if (left instanceof Comparable comparable) {
                return comparable.compareTo(right);
            }
            return 0;
        };
    }

    private record DelayedValue<T>(T value, long delayMillis, long startTime) implements Delayed {
        private DelayedValue(T value, long delayMillis) {
            this(value, delayMillis, System.currentTimeMillis());
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long delay = startTime + delayMillis - System.currentTimeMillis();
            return unit.convert(delay, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            return Long.compare(getDelay(TimeUnit.MILLISECONDS), other.getDelay(TimeUnit.MILLISECONDS));
        }
    }
}
