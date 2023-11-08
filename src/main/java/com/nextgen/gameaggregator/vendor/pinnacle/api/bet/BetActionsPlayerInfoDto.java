package com.nextgen.gameaggregator.vendor.pinnacle.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetActionsPlayerInfoDto {
    @JsonProperty("LoginId")
    private String loginId;

    @JsonProperty("UserCode")
    private String userCode;
}
