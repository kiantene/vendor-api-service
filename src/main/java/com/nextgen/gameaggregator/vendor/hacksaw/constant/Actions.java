package com.nextgen.gameaggregator.vendor.hacksaw.constant;

import java.util.Arrays;
import java.util.List;

public class Actions {

    //vendor to GA
    public static final String AUTHENTICATE = "Authenticate";

    public static final String BALANCE = "Balance";

    public static final String BET = "Bet";

    public static final String CREDIT = "Win";

    public static final String ROLLBACK = "Rollback";
    public static final String LOGOUT = "logout";

    public static final List<String> actionsList = Arrays.asList(
            AUTHENTICATE,
            BALANCE,
            BET,
            CREDIT,
            ROLLBACK
    );
}
