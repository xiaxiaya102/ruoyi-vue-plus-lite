package org.dromara.common.redis.utils;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.core.utils.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Process-local sequence generator.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SequenceUtils {

    public static final long DEFAULT_INIT_VALUE = 1L;
    public static final long DEFAULT_STEP_VALUE = 1L;
    public static final Duration DEFAULT_EXPIRE_TIME_DAY = Duration.ofDays(1);
    public static final Duration DEFAULT_EXPIRE_TIME_MINUTE = Duration.ofMinutes(1);
    public static final int DEFAULT_MIN_ID_CAPACITY_BITS = 6;

    private static final ConcurrentHashMap<String, AtomicLong> SEQUENCES = new ConcurrentHashMap<>();

    public static long getNextId(String key, Duration expireTime, long initValue, long stepValue) {
        long start = initValue <= 0 ? DEFAULT_INIT_VALUE : initValue;
        long step = stepValue <= 0 ? DEFAULT_STEP_VALUE : stepValue;
        AtomicLong sequence = SEQUENCES.computeIfAbsent(key, k -> new AtomicLong(start - step));
        return sequence.addAndGet(step);
    }

    public static long getNextId(String key, Duration expireTime) {
        return getNextId(key, expireTime, DEFAULT_INIT_VALUE, DEFAULT_STEP_VALUE);
    }

    public static String getNextIdString(String key, Duration expireTime, long initValue, long stepValue) {
        return Convert.toStr(getNextId(key, expireTime, initValue, stepValue));
    }

    public static String getNextIdString(String key, Duration expireTime) {
        return Convert.toStr(getNextId(key, expireTime));
    }

    public static String getPaddedNextIdString(String key, Duration expireTime, Integer width) {
        return StringUtils.leftPad(getNextIdString(key, expireTime), width, '0');
    }

    @Deprecated
    public static String getDateId() {
        return getDateId("");
    }

    public static String getDateId(String prefix) {
        return getDateId(prefix, true);
    }

    public static String getDateId(String prefix, boolean isWithPrefix) {
        return getDateId(prefix, isWithPrefix, -1);
    }

    public static String getPaddedDateId(String prefix, boolean isWithPrefix) {
        return getDateId(prefix, isWithPrefix, DEFAULT_MIN_ID_CAPACITY_BITS);
    }

    public static String getDateId(String prefix, boolean isWithPrefix, int minIdCapacityBits) {
        return getDateId(prefix, isWithPrefix, minIdCapacityBits, LocalDate.now());
    }

    public static String getDateId(String prefix, boolean isWithPrefix, int minIdCapacityBits, LocalDate time) {
        return getDateId(prefix, isWithPrefix, minIdCapacityBits, time, DEFAULT_INIT_VALUE, DEFAULT_STEP_VALUE);
    }

    public static String getDateId(String prefix, boolean isWithPrefix, int minIdCapacityBits,
                                   LocalDate time, long initValue, long stepValue) {
        String date = time.format(DateTimeFormatter.BASIC_ISO_DATE);
        return buildId(prefix, isWithPrefix, minIdCapacityBits, date,
            getNextId(prefix + date, DEFAULT_EXPIRE_TIME_DAY, initValue, stepValue));
    }

    public static String getDateTimeId(String prefix) {
        return getDateTimeId(prefix, true);
    }

    public static String getDateTimeId(String prefix, boolean isWithPrefix) {
        return getDateTimeId(prefix, isWithPrefix, -1);
    }

    public static String getPaddedDateTimeId(String prefix, boolean isWithPrefix) {
        return getDateTimeId(prefix, isWithPrefix, DEFAULT_MIN_ID_CAPACITY_BITS);
    }

    public static String getDateTimeId(String prefix, boolean isWithPrefix, int minIdCapacityBits) {
        return getDateTimeId(prefix, isWithPrefix, minIdCapacityBits, LocalDateTime.now());
    }

    public static String getDateTimeId(String prefix, boolean isWithPrefix, int minIdCapacityBits, LocalDateTime time) {
        return getDateTimeId(prefix, isWithPrefix, minIdCapacityBits, time, DEFAULT_INIT_VALUE, DEFAULT_STEP_VALUE);
    }

    public static String getDateTimeId(String prefix, boolean isWithPrefix, int minIdCapacityBits,
                                       LocalDateTime time, long initValue, long stepValue) {
        String dateTime = time.format(DateTimeFormatter.ofPattern(DatePattern.PURE_DATETIME_PATTERN));
        return buildId(prefix, isWithPrefix, minIdCapacityBits, dateTime,
            getNextId(prefix + dateTime, DEFAULT_EXPIRE_TIME_MINUTE, initValue, stepValue));
    }

    public static String getTemporalId(String prefix, boolean isWithPrefix, int minIdCapacityBits,
                                       TemporalAccessor time, DateTimeFormatter formatter) {
        String temporal = formatter.format(time);
        return buildId(prefix, isWithPrefix, minIdCapacityBits, temporal,
            getNextId(prefix + temporal, DEFAULT_EXPIRE_TIME_DAY));
    }

    private static String buildId(String prefix, boolean isWithPrefix, int minIdCapacityBits, String timePart, long id) {
        String idString = minIdCapacityBits > 0 ? StringUtils.leftPad(Convert.toStr(id), minIdCapacityBits, '0') : Convert.toStr(id);
        return (isWithPrefix ? prefix : "") + timePart + idString;
    }
}
