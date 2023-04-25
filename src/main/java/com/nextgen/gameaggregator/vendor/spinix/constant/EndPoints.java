package com.nextgen.gameaggregator.vendor.spinix.constant;

public class EndPoints {

    public static final Integer TIMEOUT = 10000;

    public static final String PATH = "api/v1/spinix";
    public static final String BALANCE = "/users/getBalance";
    public static final String ROUND = "/round/payout";

    // API url call to vendor
    public static final String GAME_URL = "/games/getGameUrl";
    public static final String GAME_LOBBY = "/games/getGameLobby";
    public static final String GAME_RESULT = "/games/getGameResultUrl";

    // API url to get game list
    public static final String GAME_LIST = "/games/getGameList";
}
