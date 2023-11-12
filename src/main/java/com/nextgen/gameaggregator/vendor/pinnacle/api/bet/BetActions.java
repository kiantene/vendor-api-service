package com.nextgen.gameaggregator.vendor.pinnacle.api.bet;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class BetActions {
    @JsonProperty("Id")
    private Long id;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("Transaction")
    private BetActionsTransactionDto transaction;

    @JsonProperty("PlayerInfo")
    private BetActionsPlayerInfoDto playerInfo;

    @JsonProperty("WagerInfo")
    private BetActionsWagerInfoDto wagerInfo;
}
