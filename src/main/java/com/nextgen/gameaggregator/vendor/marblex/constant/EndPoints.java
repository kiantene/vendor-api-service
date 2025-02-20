package com.nextgen.gameaggregator.vendor.marblex.constant;

public class EndPoints {
    public static final Integer TIMEOUT = 10000;

    public static final Integer RETRY = 3;

    // Vendor Path
    public static final String PATH = "api/v1/marblex";

    // API url call from vendor
    public static final String BALANCE = "/Balance";
    public static final String BET = "/Bet";
    public static final String RESULT = "/BetResult";
    public static final String CANCEL = "/Cancel";
    public static final String VOID = "/Void";
    public static final String RESETTLE = "/ResettleResult";

    // API url call to vendor
    public static final String GAME_URL = "/LaunchGame";
    public static final String BET_DETAIL_URL = "/GetGameResult";
}
