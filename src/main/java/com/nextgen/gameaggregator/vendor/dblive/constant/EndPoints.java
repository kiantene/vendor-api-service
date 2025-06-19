package com.nextgen.gameaggregator.vendor.dblive.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EndPoints {
    //GA Path
    public static final String PATH = "api/v1/dblive";

    public static final String BALANCE = "getBalance";

    public static final String BATCH_BALANCE = "getBatchBalance";

    public static final String BET_CONFIRM = "betConfirm";

    public static final String GAME_PAYOUT = "gamePayout";

    public static final String BET_CANCEL = "betCancel";

    public static final String ACTIVITY_PAYOUT = "activityPayout";

    //Vendor Uri Path
    public static final String CREATE_PLAYER = "/api/merchant/create/v2";

    public static final String LAUNCH_GAME = "/api/merchant/forwardGame/v2";
}
