package com.nextgen.gameaggregator.vendor.bglive.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class QueryStatus {

    public static final Integer UNSETTLE_BET = 0;
    public static final Integer NORMAL_SETTLE = 1;
    public static final Integer SETTLE_ERROR = 2;
    public static final Integer GAME_INTERRUPT = 4;
    public static final Integer ADMIN_INTERRUPT = 8;
}
