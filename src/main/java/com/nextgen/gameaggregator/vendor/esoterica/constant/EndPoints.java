package com.nextgen.gameaggregator.vendor.esoterica.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EndPoints {
    public static final String CLASS_NAME = "esoterica";
    public static final String PATH = "/api/v1/" + CLASS_NAME;
    public static final String AUTHENTICATE = "/authenticate";
    public static final String BALANCE = "/balance";
    public static final String BET = "/bet";
    public static final String REFUND = "/refund";
    public static final String RESULT = "/result";
    public static final String BETANDRESULT = "/betAndResult";
    public static final String ENDROUND = "/endRound";
    public static final String LAUNCH_PATH = "/api/v2/game/url";

}
