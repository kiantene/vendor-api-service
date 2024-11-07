package com.nextgen.gameaggregator.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RedisKeyConstant {
    public static final String END_ROUND_REDIS_KEY = "EndRound:%s_%s_%s";

    public static final String END_ROUND_RETRY_COUNTER_REDIS_KEY = "EndRound:RetryCounter:%s_%s_%s";
}
