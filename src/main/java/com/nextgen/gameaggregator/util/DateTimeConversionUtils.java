package com.nextgen.gameaggregator.util;

import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@UtilityClass
public class DateTimeConversionUtils {

    private static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("GMT+8");

    /**
     * Convert datetime string to Unix timestamp in milliseconds.
     *
     * @param datetimeStr datetime string (e.g., "2025-04-23 15:30:00")
     * @return unix timestamp in milliseconds
     */
    public static long toUnixTimestamp(String datetimeStr) {
        return toUnixTimestamp(datetimeStr, DEFAULT_PATTERN, DEFAULT_ZONE);
    }

    /**
     * Convert datetime string to Unix timestamp in milliseconds (custom pattern + zone).
     *
     * @param datetimeStr the datetime string to convert
     * @param pattern     the date pattern (e.g. "yyyy-MM-dd HH:mm:ss")
     * @param zone        the timezone to apply (e.g. ZoneId.of("GMT+0"))
     * @return the Unix timestamp in milliseconds
     */
    public static long toUnixTimestamp(String datetimeStr, String pattern, ZoneId zone) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern).withZone(zone);
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(datetimeStr, formatter);
        return zonedDateTime.toInstant().toEpochMilli();
    }

    /**
     * Convert Unix timestamp (milliseconds) to formatted string using default zone and pattern.
     *
     * @param timestamp the Unix timestamp to convert (milliseconds or seconds)
     * @return the formatted date string
     */
    public static String fromUnixTimestamp(long timestamp) {
        return fromUnixTimestamp(timestamp, DEFAULT_PATTERN, DEFAULT_ZONE);
    }

    /**
     * Convert Unix timestamp (milliseconds) to formatted string (custom pattern + zone).
     *
     * @param timestamp the Unix timestamp to convert (milliseconds or seconds)
     * @param pattern   the date pattern (e.g. "yyyy-MM-dd HH:mm:ss")
     * @param zone      the timezone to apply (e.g. ZoneId.of("GMT+0"))
     * @return the formatted date string
     */
    public static String fromUnixTimestamp(long timestamp, String pattern, ZoneId zone) {
        ZonedDateTime zonedDateTime = Instant.ofEpochMilli(timestamp).atZone(zone);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return formatter.format(zonedDateTime);
    }

    /**
     * Normalize a Unix timestamp to milliseconds.
     * Automatically detects whether the input is in seconds or milliseconds.
     *
     * @param timestamp the input timestamp (in seconds or milliseconds)
     * @return timestamp converted to milliseconds
     */
    public static long normalizeToMilliseconds(long timestamp) {
        // Seconds usually have 10 digits; anything shorter is definitely seconds
        // Add an extra safety check: timestamps < year 2000 are considered seconds
        long absTimestamp = Math.abs(timestamp);
        return (absTimestamp < 1_000_000_000_000L) ? timestamp * 1000 : timestamp;
    }

    /**
     * Get the current time as a Unix timestamp in milliseconds, based on the given timezone.
     *
     * @param zone the timezone to apply (e.g. ZoneId.of("GMT+8"))
     * @return the current time in milliseconds
     */
    public static long toCurrentUnixTimestampWithTimeZone(ZoneId zone) {
        ZonedDateTime zonedDateTime = ZonedDateTime.now(zone);
        return zonedDateTime.toInstant().toEpochMilli();
    }
}
