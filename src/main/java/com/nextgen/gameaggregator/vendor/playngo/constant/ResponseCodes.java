package com.nextgen.gameaggregator.vendor.playngo.constant;

import java.util.HashMap;
import java.util.Map;

public class ResponseCodes {

    public static final String OK = "0";
    public static final String INTERNAL = "2";

    public static final Map<String, String> RESPONSE_DESCRIPTION = new HashMap<>() {{
        put(OK, "OK");
        put(INTERNAL, "INTERNAL");
    }};


}
