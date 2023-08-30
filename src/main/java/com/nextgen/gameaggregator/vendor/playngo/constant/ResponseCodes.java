package com.nextgen.gameaggregator.vendor.playngo.constant;

import java.util.HashMap;
import java.util.Map;

public class ResponseCodes {

    public static final Integer OK = 0;
    public static final Integer NOUSER = 1;
    public static final Integer INTERNAL = 2;
    public static final Integer INVALIDCURRENCY = 3;
    public static final Integer WRONGUSERNAMEPASSWORD = 4;
    public static final Integer ACCOUNTLOCKED = 5;
    public static final Integer ACCOUNTDISABLED = 6;
    public static final Integer NOTENOUGHMONEY = 7;
    public static final Integer MAXCONCURRENTCALLS = 8;
    public static final Integer SPENDINGBUDGETEXCEEDED = 9;
    public static final Integer SESSIONEXPIRED = 10;
    public static final Integer TIMEBUDGETEXCEEDED = 11;
    public static final Integer SERVICEUNAVAILABLE = 12;


    public static final Map<Integer, String> RESPONSE_DESCRIPTION = new HashMap<>() {{
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
