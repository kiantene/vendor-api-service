package com.nextgen.gameaggregator.vendor.saba.constant;

import lombok.experimental.UtilityClass;

import java.time.ZoneId;

@UtilityClass
public class DateTime {
    public static final String PATTERN_WIN_LOST_DATE = "yyyy-MM-dd'T'HH:mm:ss[.SSS][XXX]";
    public static final String PATTERN_SETTLEMENT_TIME = "yyyy-MM-dd'T'HH:mm:ss[.SSS]";
    public static final ZoneId ZONE = ZoneId.of("GMT-4");
}