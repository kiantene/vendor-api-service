package com.nextgen.gameaggregator.vendor.koolbet.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EndPoints {

    public static final String PATH = "api/v1/koolbet";

    public static final String TOKEN = "/auth";

    public static final String BALANCE = "/balance";

    public static final String BET = "/bet";

    public static final String CANCEL_BET = "/cancelBet";

    public static final String SESSION_BET = "/sessionBet";

    public static final String CANCEL_SESSION_BET = "/cancelSessionBet";

    public static final String REWARD = "/reward";

    // API url call to vendor
    public static final String GAME_URL = "/singleWallet/LoginWithoutRedirect";
}
