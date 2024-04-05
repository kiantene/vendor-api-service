package com.nextgen.gameaggregator.vendor.saba.constant;

public class EndPoints {
    public static final Integer TIMEOUT = 10000;
    public static final Integer RETRY = 3;
    public static final String VENDOR_CODE = "SABA";

    // Vendor Path
    public static final String PATH = "api/v1/saba";

    // Call To Vendor
    public static final String GAME_URL = "/GetSabaUrl";
    public static final String CREATE_MEMBER = "/CreateMember";

    // Single Bet and Cash Out
    public static final String GET_BALANCE = "/getbalance";
    public static final String PLACE_BET = "/placebet";
    public static final String CONFIRM_BET = "/confirmbet";
    public static final String CANCEL_BET = "/cancelbet";
    public static final String SETTLE = "/settle";
    public static final String RESETTLE = "/resettle";
    public static final String UNSETTLE = "/unsettle";

    // Parlay Bet
    public static final String PLACE_BET_PARLAY = "/placebetparlay";
    public static final String CONFIRM_BET_PARLAY = "/confirmbetparlay";

    // Wallet
    public static final String ADJUST_BALANCE = "/adjustbalance";

    // Bet Details
    public static final String BET_DETAIL = "/GetBetDetailByTransID";
}
