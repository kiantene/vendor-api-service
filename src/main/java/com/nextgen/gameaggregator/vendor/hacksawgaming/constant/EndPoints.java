package com.nextgen.gameaggregator.vendor.hacksawgaming.constant;

public class EndPoints {

    public static final Integer TIMEOUT = 10000;

    public static final String PATH = "api/v1/hacksaw";
    public static final String BALANCE = "/getBalance";
    public static final String LOGIN = "/login";
    public static final String WAGER = "/wager";
    public static final String CANCEL_WAGER = "/cancelWager";
    public static final String APPEND_WAGER = "/appendWager";
    public static final String END_WAGER = "/endWager";
    public static final String FREE_SPIN_RESULT = "/freeSpinResult";

    // API url call to vendor
    public static final String GAME_URL = "/dcs/loginGame";
    public static final String GAME_RESULT = "/dcs/getReplay";
}
