package com.nextgen.gameaggregator.vendor.api.pgsoft.component.constant;

public class Constant {
    public static final String VENDOR_CODE = "PG";

    public static final String API_VERSION = "v1";
    public static final String WEB_ACTION = "api/" + API_VERSION + "/pgsoft/";

    //* Callback Endpoints
    public static final String ACTION_AUTHENTICATE = "VerifySession";
    public static final String ACTION_BALANCE = "Cash/Get";
    public static final String ACTION_BET = "Cash/TransferInOut";
//    public static final String ACTION_BONUS_WIN = "bonusWin";
//    public static final String ACTION_JACKPOT_WIN = "jackpotWin";
//    public static final String ACTION_PROMO_WIN = "promoWin";
//    public static final String ACTION_REFUND = "refund";
    public static final String ACTION_RESULT = "Cash/TransferInOut";
//    public static final String ACTION_END_ROUND = "endRound";
}
