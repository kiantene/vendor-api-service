package com.nextgen.gameaggregator.vendor.gpkiconic.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class BetType {

    // place bet
    public static final String POINTIN = "2";

    // win money
    public static final String POINTOUT = "1";


    public static boolean isTips(String value) {
        if (value == null) {
            return false;
        }
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }
}
