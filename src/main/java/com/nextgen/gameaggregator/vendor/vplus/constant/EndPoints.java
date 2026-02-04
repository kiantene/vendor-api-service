package com.nextgen.gameaggregator.vendor.vplus.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EndPoints {

    public static final String CLASS_NAME = "vplus";
    public static final String PATH = "/api/v1/" + CLASS_NAME;
    public static final String CREATE_USER = "/createAccount";
    public static final String LOGIN_USER = "/player/login";
    public static final String BALANCE = "/getbalance";
    public static final String BET = "/bet";
    public static final String SETTLEMENT = "/settlement";

}
