package com.nextgen.gameaggregator.vendor.cockfight6.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EndPoints {
    public static final String CLASS_NAME = "cockfight6";
    public static final String PATH = "/api/v1/" + CLASS_NAME;
    public static final String BALANCE = "/queryBalance";
    public static final String ACTION = "/changePlayerWallet";
    public static final String GAME_URL = "/v1/start_game";
}
