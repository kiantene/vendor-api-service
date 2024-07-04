package com.nextgen.gameaggregator.vendor.pinnacle.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Action {
    @JsonProperty("Id")
    private Long id;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("Transaction")
    private ActionsTransactionDto transaction;

    @JsonProperty("PlayerInfo")
    private ActionsPlayerInfoDto playerInfo;

    @JsonProperty("WagerInfo")
    @NotNull(message = "WagerInfo cannot be null")
    private ActionsWagerInfoDto wagerInfo;

}
