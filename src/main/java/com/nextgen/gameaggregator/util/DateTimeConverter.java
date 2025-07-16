package com.nextgen.gameaggregator.util;

import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@UtilityClass
public class DateTimeConverter {
    // Predefined common date formats as static string constants
    public static final String ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'"; // 2024-02-21T12:34:56Z
    public static final String ISO_LOCAL = "yyyy-MM-dd'T'HH:mm:ss"; // 2024-02-21T12:34:56 (without 'Z')
    public static final String STANDARD = "yyyy-MM-dd HH:mm:ss"; // 2024-02-21 12:34:56
    public static final String US_FORMAT = "MM/dd/yyyy HH:mm:ss"; // 02/21/2024 12:34:56
    public static final String EU_FORMAT = "dd-MM-yyyy HH:mm:ss"; // 21-02-2024 12:34:56
    public static final String RFC_1123 = "EEE, dd MMM yyyy HH:mm:ss z"; // Wed, 21 Feb 2024 12:34:56 GMT

    /**
     * Converts a datetime string to a Unix timestamp (milliseconds).
     *
     * @param datetimeString the datetime string to be converted
     * @param format the format pattern that the datetimeString is in
     * @return the Unix timestamp in milliseconds
     */
    public static long convertToTimestamp(String datetimeString, String format) {
        // If datetimeString or format is null or empty, return the current timestamp
        if (datetimeString == null || datetimeString.trim().isEmpty() || format == null || format.trim().isEmpty()) {
            return Instant.now().toEpochMilli(); // Parse failed, return current timestamp
        }

        try {
            // Parse the datetimeString using the given format pattern
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            LocalDateTime localDateTime = LocalDateTime.parse(datetimeString, formatter);
            // Convert LocalDateTime to Unix timestamp (milliseconds) using system default time zone
            return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            // If parsing fails, return the current timestamp
            return Instant.now().toEpochMilli(); // Parse failed, return current timestamp
        }
    }
}
