package com.nextgen.gameaggregator.vendor.gpkpushgaming.constant;

import java.util.HashMap;
import java.util.Map;

public class ResponseCodes {
    public static final int SUCCESS = 0;

    public static final int INSUFFICIENT_BALANCE = 1085;

    public static final int ERROR = 1001;

    public static final Map<Integer, String> RESPONSE_DESCRIPTION = new HashMap<>() {{
        put(SUCCESS, "Success");
        put(INSUFFICIENT_BALANCE, "The transfer of account failed, as the account balance is insufficient");
        put(ERROR, "Error");
    }};
}
