package com.nextgen.gameaggregator.vendor.facai.constant;

import lombok.experimental.UtilityClass;

import java.time.ZoneId;

@UtilityClass
public class DateTime {
    public static final String PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final ZoneId ZONE = ZoneId.of("UTC-4");
}
