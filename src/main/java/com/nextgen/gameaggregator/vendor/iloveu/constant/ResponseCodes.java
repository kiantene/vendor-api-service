package com.nextgen.gameaggregator.vendor.iloveu.constant;

import java.util.HashMap;
import java.util.Map;
public class ResponseCodes {

    public static final String SUCCESS = "S100";
    public static final String REPEATED_REQUEST = "S200";
    public static final String INVALID_SIGNATURE = "E113";
    public static final String INVALID_SN = "F0002";
    public static final String INVALID_PARAMETER = "E101";
    public static final String INVALID_METHOD = "F0009";
    public static final String SYSTEM_ERROR = "E102";
    public static final String INSUFFICIENT_BALANCE = "E119";
    public static final String RECORD_NOT_FOUND = "E106";

    public static final Map<String, String> RESPONSE_DESCRIPTION = new HashMap<>() {{
        put(SUCCESS, "Success");
        put(REPEATED_REQUEST, "Repeated request");
        put(INVALID_SIGNATURE, "Invalid signature");
        put(INVALID_SN, "Invalid SN");
        put(INVALID_PARAMETER, "Invalid parameter");
        put(INVALID_METHOD, "Invalid method");
        put(SYSTEM_ERROR, "System error");
        put(INSUFFICIENT_BALANCE, "Balance is not enough");
        put(INSUFFICIENT_BALANCE, "Record not found");
    }};


}
