package com.nextgen.gameaggregator.vendor.pinnacle.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.pinnacle.api.bet.BetActionsPlayerInfoDto;
import com.nextgen.gameaggregator.vendor.pinnacle.api.bet.BetActionsTransactionDto;
import com.nextgen.gameaggregator.vendor.pinnacle.api.bet.BetActionsWagerInfoDto;

import lombok.Data;

@Data
public class Action {
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
