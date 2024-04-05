package com.nextgen.gameaggregator.vendor.iloveu.constant;

import org.springframework.http.MediaType;

public class Formats {

    // API Header
    public static final String APPLICATION_JSON = MediaType.APPLICATION_JSON.toString();

    // API Value
    public static final Integer TIMEOUT = 10000;
    public static final Integer RETRY = 3;

    // Date Value
    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    public static final String TIME_ZONE = "UTC+8";

}
