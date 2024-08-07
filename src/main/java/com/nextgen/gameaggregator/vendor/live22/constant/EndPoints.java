package com.nextgen.gameaggregator.vendor.live22.constant;

public class EndPoints {

    public static final Integer TIMEOUT = 10000;

    public static final Integer RETRY = 3;

    // Vendor Path
    public static final String PATH = "api/v1/live22";

    // API url call from vendor
    public static final String GET_BALANCE = "/GetBalance";
    public static final String BET = "/Bet";
    public static final String GAME_RESULT = "/GameResult";
    public static final String ROLLBACK = "/Rollback";
    public static final String CASH_BONUS = "/CashBonus";

    // API url call to vendor
    public static final String GAME_URL = "/GameLogin";
    public static final String BET_DETAIL_URL = "/GetTransactionDetails";
}
