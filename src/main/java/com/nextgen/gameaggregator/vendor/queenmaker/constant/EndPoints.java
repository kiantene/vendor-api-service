package com.nextgen.gameaggregator.vendor.queenmaker.constant;

public class EndPoints {
    public static final Integer TIMEOUT = 10000;
    public static final Integer RETRY = 3;

    public static final String PATH = "api/v1/qm";

    public static final String WALLET_CREDIT = "/wallet/credit";
    public static final String WALLET_DEBIT = "/wallet/debit";
    public static final String WALLET_BALANCE = "/wallet/balance";

    // API url call to vendor
    public static final String AUTHORIZE = "/api/player/authorize";
    public static final String HISTORY = "/api/history/providers";

    public static final String GAME_URL = "/gamelauncher";
}
