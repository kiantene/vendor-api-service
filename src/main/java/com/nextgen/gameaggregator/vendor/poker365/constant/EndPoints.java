package com.nextgen.gameaggregator.vendor.poker365.constant;

public class EndPoints {
    public static final String PATH = "/api/v1/365poker";
    public static final String KEY = "/getKey";
    public static final String BET = "/bet";
    public static final String CANCEL_BET = "/cancelBet";
    public static final String LAUNCH_GAME = "/login";
    public static final String LOG_OUT = "/logout";
    public static final String BALANCE = "/getBalance";
    public static final String SETTLE = "/settle";
    public static final String BET_DETAILS = "/reports/dailyReport";
    public static final Integer TIMEOUT = 10000;

    private EndPoints() {
        throw new UnsupportedOperationException("Cannot instantiate utility class.");
    }
}
