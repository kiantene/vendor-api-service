package com.nextgen.gameaggregator.vendor.tbp.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EndPoints {

    public static final String PATH = "/api/v1/tbp";

    public static final String GAME_LIST = "/Games";

    public static final String LAUNCHER = "/launch/real";

    public static final String AUTHORIZE = "/PlayerIdentity/Authorize";

    public static final String AUTHENTICATE = "/Authenticate";

    public static final String GETBALANCE = "/GetBalance";

    public static final String WITHDRAW = "/Withdraw";

    public static final String DEPOSIT = "/Deposit";

    public static final String CANCEL = "/Cancel";

    public static final Integer TIMEOUT = 10000;

    public static final Integer RETRY = 3;
}
