package com.nextgen.gameaggregator.vendor.ifg.constant;

import org.springframework.http.MediaType;

public class Formats {

    // API Header
    public static final String APPLICATION_JSON = MediaType.APPLICATION_JSON.toString();

    // API Value
    public static final Integer TIMEOUT = 10000;
    public static final Integer RETRY = 3;

    // Date Format
    public static final String DATE_FORMAT = "yyyy-MM-dd";
}
