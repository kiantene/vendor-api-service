package com.nextgen.gameaggregator.vendor.jili.constant;

public class EndPoints {
    public static final Integer TIMEOUT = 10000;
    public static final Integer RETRY = 3;

    public static final String PATH_JILI = "api/v1/jili";
    public static final String PATH_TADA = "api/v1/tada";
    public static final String AUTH = "/auth";
    public static final String BET = "/bet";
    public static final String CANCEL_BET = "/cancelBet";
    public static final String SESSION_BET = "/sessionBet";
    public static final String CANCEL_SESSION_BET = "/cancelSessionBet";

    // API url call to vendor
    public static final String GAME_URL = "/singleWallet/LoginWithoutRedirect";
    public static final String BET_DETAIL_URL = "/GetGameDetailUrl";
}
