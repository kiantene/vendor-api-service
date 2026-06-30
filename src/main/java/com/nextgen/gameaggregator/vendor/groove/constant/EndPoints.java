package com.nextgen.gameaggregator.vendor.groove.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EndPoints {
    public static final String CLASS_NAME = "groove";
    public static final String PATH = "api/v1/" + CLASS_NAME;
    public static final String AUTHENTICATE = "request=getaccount";
    public static final String BALANCE = "request=getbalance";
    public static final String BET = "request=wager";
    public static final String RESULT = "request=result";
    public static final String ROLLBACK = "request=rollback";
    public static final String BETANDRESULT = "request=wagerAndResult";
}
