package com.nextgen.gameaggregator.vendor.pgsoft.constant;

public class Endpoints {

    public static final Integer TIMEOUT = 10000;

    public static final String PATH = "api/v1/pgsoft/";
    public static final String AUTHENTICATE = "VerifySession";
    public static final String BALANCE = "Cash/Get";
    public static final String BET = "Cash/TransferInOut";
    public static final String GAME_LIST = "Game/v2/Get";
    public static final String BET_DETAIL_STEP_ONE = "Login/v1/LoginProxy";
    public static final String BET_DETAIL_STEP_TWO = "history/redirect.html";

    public static final String GET_BET_HISTORY = "Bet/v4/GetHistory";
}
