package com.nextgen.gameaggregator.vendor.inout.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EndPoints {
    public static final String CLASS_NAME = "inout";
    public static final String PATH = "api/v1/" + CLASS_NAME;
    public static final String ACTION = "action";
    public static final String BET_DETAIL_URL = "/api/operator/v1/round-result/";
    public static final Integer TIMEOUT = 10000;
}
