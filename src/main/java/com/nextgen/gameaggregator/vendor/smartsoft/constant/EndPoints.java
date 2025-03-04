package com.nextgen.gameaggregator.vendor.smartsoft.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EndPoints {

    public static final String PATH = "/api/v1/ssg";

    public static final String SESSION = "/ActivateSession";

    public static final String BALANCE = "/GetBalance";

    public static final String DEPOSIT = "/Deposit";

    public static final String WITHDRAW = "/Withdraw";

    public static final String ROLLBACK = "/RollbackTransaction";

    public static final String ADDGIFT = "api/gifts/IssueClientGifts";

    public static final String CANCELGIFT = "api/gifts/cancelclientgift";

    public static final Integer TIMEOUT = 10000;

    public static final Integer RETRY = 3;
}
