package com.nextgen.gameaggregator.vendor.digitain.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EndPoints {
    public static final String CLASS_NAME = "digitain";
    public static final String PATH = "/api/v1/" + CLASS_NAME;
    public static final String AUTHENTICATE = "/authenticate";
    public static final String BALANCE = "/getbalance";
    public static final String BET = "/bet";
    public static final String RESULT = "/result";
    public static final String ROLLBACK = "/cancel";
    public static final String BETANDRESULT = "/betandresult";
    public static final String PROMOWIN = "/promowin";
}