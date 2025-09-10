package com.nextgen.gameaggregator.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Features {
    AGENT_MAX_PAYOUT(1, "AGENT_MAX_PAYOUT", "set agent max payout by vendor currencies and game category");

    public final Integer id;
    public final String code;
    public final String description;

}
