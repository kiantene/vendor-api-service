package com.nextgen.gameaggregator.vendor.cq9.constant;

public class EndPoints {
    public static final String PATH = "api/v1/cq9";
    public static final String GAME_URL = "/gameboy/player/sw/gamelink";
    public static final String AUTHENTICATE = "/player/check/{account}";
    public static final String BALANCE = "/transaction/balance/{account}";
    public static final String BET = "/transaction/game/bet";
    public static final String END_ROUND = "/transaction/game/endround";
    public static final String REFUND = "/transaction/game/refund";
}
