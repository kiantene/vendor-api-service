package com.nextgen.gameaggregator.vendor.lucky365.constant;

import lombok.experimental.UtilityClass;
@UtilityClass
public class EndPoints {
    public static final String CLASS_NAME = "lucky365";
    public static final String PATH = "/api/v1/" + CLASS_NAME;
    public static final String BALANCE = "/GetSingleWallet";
    public static final String GAME_URL_PATH = "/UserInfo/Login";
    public static final String CREATE_PLAYER = "/UserInfo/CreatePlayer";
    public static final String BET = "/Bet";
    public static final String SETTLE = "/Settle";
    public static final String ROLLBACK = "/CancelBet";

}
