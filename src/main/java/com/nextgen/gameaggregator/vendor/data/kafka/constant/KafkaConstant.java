package com.nextgen.gameaggregator.vendor.data.kafka.constant;

public class KafkaConstant {
    public static final String GROUP_ID = "ga_bet_transformation_service";

    /* Consumer Topic */
    public static final String TOPIC_SEAMLESS_BET_TRANSFORMATION = "topic_seamless_bet_transformation";
    public static final String TOPIC_TRANSFER_BET_TRANSFORMATION = "topic_transfer_bet_transformation";

    /* Producer Topic */
    public static final String TOPIC_ERROR_HANDLING = "topic_error_handling";
    public static final String TOPIC_DATA_AGGREGATE = "topic_data_aggregate";
}
