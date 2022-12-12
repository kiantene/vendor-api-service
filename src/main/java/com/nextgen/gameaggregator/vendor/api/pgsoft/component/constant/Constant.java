package com.nextgen.gameaggregator.vendor.api.pgsoft.component.constant;

public class Constant {
    public static final String VENDOR_CODE = "PG";

    public static final String API_VERSION = "v1";
    public static final String WEB_ACTION = "api/" + API_VERSION + "/pgsoft/";

    //* Callback Endpoints
    public static final String ACTION_AUTHENTICATE = "VerifySession";
    public static final String ACTION_CASH_GET = "Cash/Get";
    public static final String ACTION_CASH_TRANSFER_IN_OUT = "Cash/TransferInOut";
    public static final String ACTION_VERIFY_SESSION = "VerifySession";
//    public static final String ACTION_END_ROUND = "endRound";
}
