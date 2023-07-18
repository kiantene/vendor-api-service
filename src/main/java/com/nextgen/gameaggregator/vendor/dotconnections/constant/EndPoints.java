package com.nextgen.gameaggregator.vendor.dotconnections.constant;

public class EndPoints {

    public static final Integer TIMEOUT = 10000;

    public static final String PATH = "api/v1/dotconnections";
    public static final String BALANCE = "/getBalance";
    public static final String LOGIN = "/login";
    public static final String WAGER = "/wager";
    public static final String CANCEL_WAGER = "/cancelWager";
    public static final String APPEND_WAGER = "/appendWager";
    public static final String END_WAGER = "/endWager";
    public static final String FREE_SPIN_RESULT = "/freeSpinResult";
    public static final String PROMO_PAYOUT = "/promoPayout";

    // API url call to vendor
    public static final String GAME_URL = "/dcs/loginGame";
    public static final String GAME_RESULT = "/dcs/getReplay";
}
