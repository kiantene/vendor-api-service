package com.nextgen.gameaggregator.vendor.crystal.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EndPoints {
    public static final String CLASS_NAME = "crystal";
    public static final String LAUNCH_GAME = "/v1/exp/launch/real";
    public static final String PATH = "/api/v1/" + CLASS_NAME;
    public static final String BALANCE = "/v1/wallet/balance";
    public static final String BET = "/v1/wallet/debit";
    public static final String SETTLE = "/v1/wallet/credit";
    public static final String REFUND = "/v1/wallet/refund";

}
