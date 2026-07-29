package com.nextgen.gameaggregator.vendor.wazdan.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EndPoints {

    public static final String CLASS_NAME = "wazdan";
    public static final String PATH = "/api/v1/" + CLASS_NAME;
    public static final String AUTHENTICATE = "/authenticate";
    public static final String BALANCE = "/getFunds";
    public static final String BET = "/getStake";
    public static final String RESULT = "/returnWin";
    public static final String ROLLBACK = "/rollbackStake";
    public static final String CLOSE = "/gameClose";
}
