package com.nextgen.gameaggregator.vendor.evoplay.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseCodes {
    SUCCESS("ok", "OK"),
    ERROR("error", "{{error}}");

    public final String status;
    public final String message;

}

//public class ResponseCodes {
//    public static final String SUCCESS = "ok";
//    public static final String ERROR = "error";
//    public static final String INTERNAL = "internal";
//    public static final String USER = "user";
//    public static final Integer INTERNAL_SERVER_ERROR = 500;
//
//
//    public static final Map<Integer, String> RESPONSE_DESCRIPTION = new HashMap<>() {{
//        put(INTERNAL_SERVER_ERROR, "Internal server error");
//    }};
//}
