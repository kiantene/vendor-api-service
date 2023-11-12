package com.nextgen.gameaggregator.vendor.pinnacle.api.accept;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AcceptActionsPlayerInfoDto {
    @JsonProperty("LoginId")
    private String loginId;

    @JsonProperty("UserCode")
    private String userCode;
}
