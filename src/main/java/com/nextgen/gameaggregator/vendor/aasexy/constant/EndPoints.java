package com.nextgen.gameaggregator.vendor.aasexy.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EndPoints {
    public static final Integer TIMEOUT = 10000;
    public static final Integer RETRY = 3;

    // Vendor Path
    public static final String PATH = "api/v1/aasexy";

    // API url call from vendor
    public static final String ACTION = "/action";

    // Call To Vendor
    public static final String GAME_URL = "/wallet/doLoginAndLaunchGame";
    public static final String CREATE_MEMBER = "wallet/createMember";

    public static final String BET_DETAIL_URL = "/wallet/getTransactionHistoryResult";

}
