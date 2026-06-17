package com.nextgen.gameaggregator.vendor.egtdigital.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EndPoints {
    public static final String CLASS_NAME = "egtdigital";
    public static final String PATH = "/api/v1/" + CLASS_NAME;
    public static final String AUTHENTICATE = "/authenticate";
    public static final String DEFENCE = "/defence-code";
    public static final String BALANCE = "/balance";
    public static final String BET = "/withdraw";
    public static final String RESULT = "/deposit";
    public static final String ROLLBACK = "/reverse/withdraw";
    public static final String TERMINATE = "/terminate";
}
