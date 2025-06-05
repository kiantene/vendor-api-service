package com.nextgen.gameaggregator.vendor.saba.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RegexPattern {
    // format in String -> yyyy-MM-dd'T'HH:mm:ss.SSSXXX (with timezone offset)
    public static final String REGEX_PATTERN_WIN_LOST_DATE = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}[+-]\\d{2}:\\d{2}$";

    // format in String -> yyyy-MM-dd'T'HH:mm:ss.SSS (no timezone)
    public static final String REGEX_PATTERN_SETTLEMENT_TIME = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}$";

}
