package com.nextgen.gameaggregator.vendor.endorphina.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EndPoints {
    public static final String CLASS_NAME = "endorphina";
    public static final String PATH = "/api/v1/" + CLASS_NAME;
    public static final String BALANCE = "/balance";
    public static final String BET = "/bet";
    public static final String SETTLE = "/win";
    public static final String REFUND = "/refund";
    public static final String BETANDRESULT = "/promoWin";
    public static final String AUTHENTICATE = "/session";
    public static final String LAUNCH_PATH = "/api/sessions/seamless/rest/v1";

}
