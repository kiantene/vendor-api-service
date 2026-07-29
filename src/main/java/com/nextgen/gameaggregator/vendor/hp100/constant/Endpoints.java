package com.nextgen.gameaggregator.vendor.hp100.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Endpoints {
    public static final String CLASS_NAME = "hp100";

    public static final String GAME_URL = "/games/v1/launch";
    public static final String PATH = "/api/v1/hp100";

    public static final String AUTHENTICATE = "/auth";
    public static final String BALANCE = "/balance";
    public static final String BET = "/withdrawal";
    public static final String RESULT = "/deposit";
    public static final String ROLLBACK = "/rollback";
}
