package com.nextgen.gameaggregator.vendor.lucky365.util;

import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

@UtilityClass
public class TimeStamp {

    private static final DateTimeFormatter FORMATTER =
            new DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd HH:mm:ss")
                    .optionalStart()
                    .appendPattern(".SSS")
                    .optionalEnd()
                    .toFormatter();

    public static long convertTimeStamp(String actionDate) {
        return LocalDateTime.parse(actionDate, FORMATTER)
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();
    }
}
