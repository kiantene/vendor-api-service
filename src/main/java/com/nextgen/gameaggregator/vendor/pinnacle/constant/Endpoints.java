package com.nextgen.gameaggregator.vendor.pinnacle.constant;

public class Endpoints {
    public static final String PATH = "api/v1/pinnacle";
    public static final String PING = "/ping";
    public static final String PLAYER_CREATE = "/player/create";
    public static final String PLAYER_LOGIN = "/player/login";
    public static final String REPORT_WAGERS = "/report/wagers";
    public static final String REPORT_ALL_WAGERS = "/report/all-wagers";
    public static final String MY_BETS_FULL = "/player/account/my-bets-full";

    public static final Integer TIMEOUT = 10000;
    public static final Integer RETRY = 3;
}
