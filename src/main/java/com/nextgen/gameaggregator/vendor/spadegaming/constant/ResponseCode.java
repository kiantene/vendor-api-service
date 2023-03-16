package com.nextgen.gameaggregator.vendor.spadegaming.constant;

import java.util.HashMap;
import java.util.Map;

public class ResponseCode {
    public static final Integer SUCCESS = 0;
    public static final Integer SYSTEM_ERROR = 1;
    public static final Integer INVALID_REQUEST = 2;
    public static final Integer SERVICE_INACCESSIBLE = 3;
    public static final Integer REQUEST_TIMEOUT = 100;
    public static final Integer CALL_LIMITED = 101;

    public static final Map<Integer, String> RESPONSE_DESCRIPTION = new HashMap<>() {{
        put(SUCCESS, "Success");
        put(SYSTEM_ERROR, "System Error");
        put(INVALID_REQUEST, "Invalid Request");
        put(SERVICE_INACCESSIBLE, "Service Inaccessible");
        put(REQUEST_TIMEOUT, "Request Timeout");
        put(CALL_LIMITED, "Call Limited");
    }};
}
