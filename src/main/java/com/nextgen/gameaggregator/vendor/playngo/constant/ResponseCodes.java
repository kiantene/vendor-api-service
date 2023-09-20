package com.nextgen.gameaggregator.vendor.playngo.constant;

import java.util.HashMap;
import java.util.Map;

public class ResponseCodes {

    public static final String OK = "0";
    public static final String NOUSER = "1";
    public static final String INTERNAL = "2";
    public static final String INVALIDCURRENCY = "3";
    public static final String WRONGUSERNAMEPASSWORD = "4";
    public static final String ACCOUNTLOCKED = "5";
    public static final String ACCOUNTDISABLED = "6";
    public static final String NOTENOUGHMONEY = "7";
    public static final String MAXCONCURRENTCALLS = "8";
    public static final String SPENDINGBUDGETEXCEEDED = "9";
    public static final String SESSIONEXPIRED = "10";
    public static final String TIMEBUDGETEXCEEDED = "11";
    public static final String SERVICEUNAVAILABLE = "12";


    public static final Map<String, String> RESPONSE_DESCRIPTION = new HashMap<>() {{
        put(OK, "ok");
        put(NOUSER, "User was not logged in.");
        put(INTERNAL, "Internal server error.");
        put(INVALIDCURRENCY, "An unsupported currency was specified.");
        put(WRONGUSERNAMEPASSWORD, "Wrong username or password.");
        put(ACCOUNTLOCKED, "Account is locked.");
        put(ACCOUNTDISABLED, "Account is disabled.");
        put(NOTENOUGHMONEY, "The requested amount is too high or too low.");
        put(MAXCONCURRENTCALLS, "The system is unavailable for this request. Try again later.");
        put(SPENDINGBUDGETEXCEEDED, "Responsible gaming limit exceeded.");
        put(SESSIONEXPIRED, "The player session has expired.");
        put(TIMEBUDGETEXCEEDED, "Responsible gaming limit exceeded.");
        put(SERVICEUNAVAILABLE, "Service is unavailable.");
    }};


}
