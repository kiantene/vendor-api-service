package com.nextgen.gameaggregator.data.kafka.constant;

public class KafkaConstant {
    /* Consumer Group ID */
    public static final String GROUP_ID = "ga_vendor_api_service";

    /* Bet Processing Topic */
    public static final String TOPIC_BET_HISTORY = "topic_bet_history";
    public static final String TOPIC_BET_HISTORY_V2 = "topic_bet_history_v2";
    public static final String TOPIC_BET_HISTORY_V3 = "topic_bet_history_v3";
    public static final String TOPIC_BET_HISTORY_V4 = "topic_bet_history_v4";
    public static final String TOPIC_BET_HISTORY_UNCAP = "topic_bet_history_uncap";
    public static final String OPERATOR_REQUEST_DLQ = "operator_request_dlq";
    public static final String TOPIC_WAREHOUSE_BET_HISTORY = "topic_warehouse_bet_history";
    public static final String TOPIC_BET_HISTORY_PREPROCESSING = "topic_bet_history_preprocessing";
    public static final String TOPIC_BET_HISTORY_PREPROCESSING_V2 = "topic_bet_history_preprocessing_v2";
    public static final String TOPIC_BET_HISTORY_PREPROCESSING_V3 = "topic_bet_history_preprocessing_v3";
    public static final String TOPIC_END_ROUND_PROCESS_V3 = "topic_end_round_process_v3";
    public static final String TOPIC_REFUND_PROCESS = "topic_refund_process";
    public static final String TOPIC_UNSETTLED_BET = "sports_unsettled_bet";
    public static final String TOPIC_MASTER_UNSETTLED_BET = "sports_master_unsettled_bet";
    public static final String TOPIC_SETTLED_BET = "sports_settled_bet";
    public static final String TOPIC_RAW_SETTLED_BET = "topic_raw_settled_bet";
    public static final String TOPIC_TRANSFER_HISTORY = "topic_transfer_history";
    public static final String TOPIC_BET_RESULT_DLQ = "topic_bet_result_dlq_v2";
    public static final String TOPIC_API_REQUEST_LOG = "api_request_log_v1";
    public static final String TOPIC_TRANSFER_WALLET_REQUEST_LOG = "transfer_wallet_request_log_v1";
    public static final String TOPIC_BET_TRANSACTION_HISTORY = "topic_bet_transaction_history";

    public static final String TOPIC_BET_HISTORY_DELAY_SETTLEMENT = "topic_bet_history_delay_settlement";
    public static final String TOPIC_BET_TRANSACTION_LOG = "topic_bet_transaction_log_v1";
    public static final String TOPIC_RESETTLEMENT_DATE_CHANGE = "topic_resettlement_date_change";
    public static final String TOPIC_PROMO_PAYOUT_HISTORY = "promo_payout_history";
    public static final String TOPIC_RECON_FOR_UNSETTLED_BET = "topic_recon_for_unsettled_bet";
    public static final String TOPIC_PATCHING_SPORT_UNSETTLED_BET_TO_REFUND_BET = "topic_patching_sport_unsettled_bet_to_refund_bet";
    public static final String TOPIC_PATCHING_SPORT_UNSETTLED_BET_TO_REFUND_BET_DLQ = "topic_patching_sport_unsettled_bet_to_refund_bet_DLQ";

    public static final String TOPIC_SETTLED_BET_DLQ = "topic_settled_bet_dlq";
    public static final String TOPIC_PROCESS_ROUND_ENDED = "topic_process_round_ended";
}
