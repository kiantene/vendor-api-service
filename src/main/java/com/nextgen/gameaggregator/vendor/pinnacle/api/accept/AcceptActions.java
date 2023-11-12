package com.nextgen.gameaggregator.vendor.pinnacle.api.accept;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class AcceptActions {
    @JsonProperty("Id")
    private Long id;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("PlayerInfo")
    private AcceptActionsPlayerInfoDto playerInfo;

    @JsonProperty("WagerInfo")
    private AcceptActionsWagerInfoDto wagerInfo;
}
