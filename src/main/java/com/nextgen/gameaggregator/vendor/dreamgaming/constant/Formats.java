package com.nextgen.gameaggregator.vendor.dreamgaming.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Formats {
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static final String JSON_LEADING_ZERO = "(?<=\":)0+(\\d+)(?=,|\\})";
}
