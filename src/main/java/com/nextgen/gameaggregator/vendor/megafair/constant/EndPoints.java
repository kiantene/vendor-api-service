package com.nextgen.gameaggregator.vendor.megafair.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EndPoints {

    public static final String CLASS_NAME = "megafair";
    public static final String PATH = "/api/v1/" + CLASS_NAME;
    public static final String LAUNCH_GAME = "/cwstartgamev2.do";
    public static final String AUTHENTICATE = "/authenticate.do";
    public static final String BETRESULT = "/betresult.do";
    public static final String REFUND = "/refundBet.do";

}
