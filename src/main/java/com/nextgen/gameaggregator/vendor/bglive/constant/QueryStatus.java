package com.nextgen.gameaggregator.vendor.bglive.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class QueryStatus {

    public static final Integer NO_BET = 0;
    public static final Integer UNSETTLE_BET = 1;
    public static final Integer SETTLE_WIN = 2;
    public static final Integer SETTLE_TIE = 3;
    public static final Integer SETTLE_LOSE = 4;
    public static final Integer CANCEL_USER = 5;
    public static final Integer CANCEL_SETTLE = 7;
}
