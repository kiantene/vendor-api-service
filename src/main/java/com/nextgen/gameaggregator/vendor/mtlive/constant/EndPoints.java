package com.nextgen.gameaggregator.vendor.mtlive.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EndPoints {

    public static final String CLASS_NAME = "mtlive";
    public static final String PATH = "/api/v1/" + CLASS_NAME;
    public static final String CREATE_USER = "Player/CreateUser";
    public static final String LAUNCH_GAME = "Player/GetURLToken";
    public static final String BALANCE = "/Balance";
    public static final String BET = "/Bet";
    public static final String RESULT = "/BetResult";
    public static final String ROLLBACK = "/CancelBet";
    public static final String ADJUSTMENT = "/ReBetResult";
    public static final String GIFT = "/Gift";
}
