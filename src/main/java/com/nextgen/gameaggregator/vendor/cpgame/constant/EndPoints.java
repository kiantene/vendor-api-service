package com.nextgen.gameaggregator.vendor.cpgame.constant;

public class EndPoints {

    public static final Integer TIMEOUT = 10000;

    public static final Integer RETRY = 3;

    public static final String PATH = "api/v1/cpgame";

    public static final String CREATE_PLAYER = "/api/login";

    public static final String LAUNCH_GAME = "/api/get_game_url";

    public static final String BALANCE = "/balance/get";

    public static final String BET = "/balance/transferInOut";

    public static final String ROLLBACK = "/balance/cancelInOut";

    public static final String UNSETTLED = "/balance/transferOut";
    public static final String SETTLED = "/balance/transferIn";
    public static final String CANCEL_BET = "/balance/cancelOut";

}
