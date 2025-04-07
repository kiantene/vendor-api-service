package com.nextgen.gameaggregator.vendor.avatarux.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EndPoints {

    public static final String PATH = "/api/v1/avatarux";

    public static final String LAUNCHER = "/launch/real";

    public static final String Authenticate = "/authenticate";

    public static final String BALANCE = "/balance";

    public static final String TRANSACTION = "/transaction";

    public static final String CANCEL = "/cancel";

    public static final Integer TIMEOUT = 10000;

    public static final Integer RETRY = 3;
}
