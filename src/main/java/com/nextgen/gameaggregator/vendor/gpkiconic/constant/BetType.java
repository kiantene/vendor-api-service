package com.nextgen.gameaggregator.vendor.gpkiconic.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class BetType {
    public static final String FINISHED = "1";

    public static final String UNFINISHED = "0";

    public static final String NOTTIPS = "0";

    // place bet
    public static final String POINTIN = "2";

    // win money
    public static final String POINTOUT = "1";


    public static boolean isTips(String value) {
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }
}
