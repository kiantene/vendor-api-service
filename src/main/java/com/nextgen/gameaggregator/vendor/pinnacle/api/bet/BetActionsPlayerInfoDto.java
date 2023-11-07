package com.nextgen.gameaggregator.vendor.pinnacle.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetActionsPlayerInfoDto {
    private String LoginId;
    private String UserCode;
}
