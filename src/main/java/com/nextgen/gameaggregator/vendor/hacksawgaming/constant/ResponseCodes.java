package com.nextgen.gameaggregator.vendor.hacksawgaming.constant;

import java.util.HashMap;
import java.util.Map;

public class ResponseCodes {
    public static final Integer SUCCESS = 1000;
    public static final Integer SYSTEM_ERROR = 1001;

    public static final Map<Integer, String> RESPONSE_DESCRIPTION = new HashMap<>() {{
        put(SUCCESS, "Success.");
        put(SYSTEM_ERROR, "System error.");
    }};
}
