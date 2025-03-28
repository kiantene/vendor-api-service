package com.nextgen.gameaggregator.vendor.playtech.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EndPoints {

    public static final String PATH = "/api/v1/playtech";
    public static final String AUTH_PATH = "/authenticate";
    public static final String BET = "/bet";
    public static final String LAUNCH_GAME = "/from-operator/getGameLaunchUrl";
    public static final String LOG_OUT = "/logout";
    public static final String BALANCE = "/getbalance";
    public static final String RESULT = "/gameroundresult";
    public static final Integer TIMEOUT = 10000;
}
