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
     */
    public static long toUnixTimestamp(String datetimeStr, String pattern, ZoneId zone) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern).withZone(zone);
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(datetimeStr, formatter);
        return zonedDateTime.toInstant().toEpochMilli();
    }

    /**
     * Convert Unix timestamp (milliseconds) to formatted string using default zone and pattern.
     */
    public static String fromUnixTimestamp(long timestamp) {
        return fromUnixTimestamp(timestamp, DEFAULT_PATTERN, DEFAULT_ZONE);
    }

    /**
     * Convert Unix timestamp (milliseconds) to formatted string (custom pattern + zone).
     */
    public static String fromUnixTimestamp(long timestamp, String pattern, ZoneId zone) {
        ZonedDateTime zonedDateTime = Instant.ofEpochMilli(timestamp).atZone(zone);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return formatter.format(zonedDateTime);
    }
}
