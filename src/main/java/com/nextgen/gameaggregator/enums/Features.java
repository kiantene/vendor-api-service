package com.nextgen.gameaggregator.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Features {
    AGENT_MAX_PAYOUT(1, "AGENT_MAX_PAYOUT", "set agent max payout by vendor currencies and game category"),
    AGENT_END_ROUND(3, "AGENT_END_ROUND", "fire end round request to agent"),
    RAW_SPORTS_BET_DETAILS_EMIT(5, "RAW_SPORTS_BET_DETAILS_EMIT", "emit raw sports bet-detail events to the Kafka topic for downstream canonicalization"),
    RAW_BET_DETAILS_EMIT(6, "RAW_BET_DETAILS_EMIT", "emit raw livecasino bet-detail events to the Kafka topic for downstream canonicalization");

    public final Integer id;
    public final String code;
    public final String description;

}
