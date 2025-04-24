package com.nextgen.gameaggregator.vendor.ygg.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EndPoints {

    public static final String PATH = "api/v1/ygg/";

    public static final String AUTHENTICATE = "playerinfo.json";

    public static final String BALANCE = "getbalance.json";

    public static final String BET = "wager.json";

    public static final String CANCEL_BET = "cancelwager.json";

    public static final String BONUS_GAME = "appendwagerresult.json";

    public static final String SETTLED = "endwager.json";

}
