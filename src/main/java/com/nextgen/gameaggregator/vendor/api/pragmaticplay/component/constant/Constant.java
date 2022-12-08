package com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.constant;

public class Constant {

    public static final String VENDOR_CODE = "PP";
    public static final String API_VERSION = "v1";
    public static final String WEB_ACTION = "api/" + API_VERSION + "/prammaticplay/";


    //region vendor incoming APIs
    public static final String ACTION_AUTHENTICATE = "authenticate";
    public static final String ACTION_BALANCE = "balance";
    public static final String ACTION_BET = "bet";
    public static final String ACTION_BONUS_WIN = "bonusWin";
    public static final String ACTION_JACKPOT_WIN = "jackpotWin";
    public static final String ACTION_PROMO_WIN = "promoWin";
    public static final String ACTION_REFUND = "refund";
    public static final String ACTION_RESULT = "result";
    public static final String ACTION_END_ROUND = "endRound";






    //endregion


    //region vendor outgoing APIs
    public static final String SEAMLESS_GAME_LOGIN = "/game/url";
    //endregion
}
