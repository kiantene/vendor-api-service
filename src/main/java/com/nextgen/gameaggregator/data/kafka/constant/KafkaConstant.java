package com.nextgen.gameaggregator.data.kafka.constant;

public class KafkaConstant {
    /* Consumer Group ID */
    public static final String GROUP_ID = "ga_vendor_api_service";

    /* Bet Processing Topic */
    public static final String TOPIC_BET_HISTORY = "topic_bet_history";
    public static final String TOPIC_BET_HISTORY_V2 = "topic_bet_history_v2";
    public static final String OPERATOR_REQUEST_DLQ = "operator_request_dlq";
    public static final String TOPIC_WAREHOUSE_BET_HISTORY = "topic_warehouse_bet_history";
    public static final String TOPIC_BET_HISTORY_PREPROCESSING = "topic_bet_history_preprocessing";
    public static final String TOPIC_END_ROUND_PROCESS_V2 = "topic_end_round_process_v2";
    public static final String TOPIC_UNSETTLED_BET = "sports_unsettled_bet";
    public static final String TOPIC_MASTER_UNSETTLED_BET = "sports_master_unsettled_bet";
    public static final String TOPIC_SETTLED_BET = "sports_settled_bet";
    public static final String TOPIC_RAW_SETTLED_BET = "topic_raw_settled_bet";
    public static final String TOPIC_TRANSFER_HISTORY = "topic_transfer_history";
    public static final String TOPIC_BET_RESULT_DLQ = "topic_bet_result_dlq_v2";
    public static final String TOPIC_API_REQUEST_LOG = "api_request_log_v1";

    public static final String TOPIC_BET_HISTORY_DELAY_SETTLEMENT = "topic_bet_history_delay_settlement";
}
