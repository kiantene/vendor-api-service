package com.nextgen.gameaggregator.vendor.casinogate.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Endpoints {
    public static final String CLASS_NAME = "casinogate";
    public static final String PATH = "/api/v1/" + CLASS_NAME + "/v1/providers/{platformId}";
    public static final String BALANCE = "/wallet";
    public static final String BET = "/bet/place";
    public static final String WIN = "/bet/win";
    public static final String REFUND = "/bet/refund";

    public static final String LAUNCH_PATH = "/game-launch";
}
