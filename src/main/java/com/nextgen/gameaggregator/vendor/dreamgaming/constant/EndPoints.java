package com.nextgen.gameaggregator.vendor.dreamgaming.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EndPoints {

    public static final String PATH = "/api/v1/dg";

    public static final String TRANSFER = "/v2/specification/account/transfer/{agentName}";

    public static final String CHECKNCOMPLETE = "/v2/specification/account/inform/{agentName}";

    public static final String BALANCE = "/v2/specification/user/getBalance/{agentName}";

    public static final String SIGNUP = "/v2/wallet/signup";

    public static final String LOGIN = "/v2/wallet/login";

    public static final Integer TIMEOUT = 10000;

    public static final Integer RETRY = 3;
}
