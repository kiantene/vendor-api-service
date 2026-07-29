package com.nextgen.gameaggregator.vendor.topbet.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EndPoints {

    public static final String CLASS_NAME = "topbet";
    public static final String PATH = "/api/v1/" + CLASS_NAME;
    public static final String CREATE_USER = "/REGISTER";
    public static final String LOGIN_USER = "/LOGIN";
    public static final String HEALTH = "/health";
    public static final String BALANCE = "/balance";
    public static final String BET = "/deductfunds";
    public static final String RESULT = "/increasefunds";
    public static final String ROLLBACK = "/rollback";

}
