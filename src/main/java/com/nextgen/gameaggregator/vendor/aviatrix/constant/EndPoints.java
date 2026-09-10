package com.nextgen.gameaggregator.vendor.aviatrix.constant;

public class EndPoints {
    public static final String CLASS_NAME = "aviatrix";
    public static final String PATH = "/api/v1/" + CLASS_NAME;
    public static final String GAME_URL = "/game/url";
    public static final String PLAYER_INFO = "/playerInfo";
    public static final String BET = "/bet";
    public static final String WIN = "/win"; //is actually result
    public static final String HEALTH = "/health";
    public static final String BONUS = "/transactions/promoWin";
    public static final String GAME_ROUND = "/game/round";
    public static final String CLOSE_MATCH = "/closeMatch";

    private EndPoints() {
    }
}
