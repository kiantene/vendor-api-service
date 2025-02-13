package com.nextgen.gameaggregator.vendor.whitecliff.constant;

public class EndPoints {

    public static final String PATH = "api/v1/whitecliff";

    public static final String API_URL = "/auth";

    public static final String LAUNCH_GAME = "/auth";

    public static final String BALANCE = "/balance";

    public static final String DEBIT = "/debit";

    public static final String CREDIT = "/credit";

    public static final String BONUS = "/bonus";

    public static final String BET_DETAIL_URL = "/bet/results";

    public static final Integer TIMEOUT = 10000;

    private EndPoints() {
    }


    public static EndPoints createEndPoints() {
        return new EndPoints();
    }
}
