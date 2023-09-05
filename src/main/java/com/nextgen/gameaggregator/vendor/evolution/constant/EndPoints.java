package com.nextgen.gameaggregator.vendor.evolution.constant;

public class EndPoints {
    public static final Integer TIMEOUT = 10000;
    public static final Integer RETRY = 3;

    public static final String PATH = "api/v1/netent";

    public static final String SID = "/sid"; // For testing purposes following service should be implemented on test environments
    public static final String CHECK = "/check";
    public static final String BALANCE = "/balance";
    public static final String DEBIT = "/debit";
    public static final String CREDIT = "/credit";
    public static final String CANCEL = "/cancel";
    public static final String GAME_PATH = "/ua/v1/";
}
