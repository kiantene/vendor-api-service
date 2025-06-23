package com.nextgen.gameaggregator.vendor.kypoker.constant;

public class Actions {

    // Vendor To GA
    public static final String LOGIN = "0";
    public static final String REFUND = "refund";
    public static final String ROLLBACK = "rollback";

    public static final int BALANCE = 1001;
    public static final int BET = 1002;
    public static final int SETTLE = 1003;
    public static final int CANCEL = 1005;
    public static final int GET_ORDER_STATUS = 1004;

    // GA to Vendor
    public static final int GAME_URL = 21;
    public static final int BET_DETAIL_URL = 54;
}
