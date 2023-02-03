package com.nextgen.gameaggregator.vendor.cq9.constant;

public class EndPoints {
    public static final String PATH = "api/v1/cq9";
    public static final String AUTHENTICATE = "/player/check/{account}";
    public static final String BALANCE = "/transaction/balance/{account}";
    public static final String BET = "/transaction/game/bet";
    public static final String END_ROUND = "/transaction/game/endround";
    public static final String REFUND = "/transaction/game/refund";
    public static final String DEBIT = "/transaction/game/debit";
    public static final String CREDIT = "/transaction/game/credit";
    public static final String PAYOFF = "/transaction/user/payoff";
    public static final String ROLLOUT = "/transaction/game/rollout";
    public static final String TAKE_ALL = "/transaction/game/takeall";
    public static final String ROLLIN = "/transaction/game/rollin";

    // API url call to vendor
    public static final String GAME_URL = "/gameboy/player/sw/gamelink";
    public static final String ORDER_RECORD = "/gameboy/order/record";
}
