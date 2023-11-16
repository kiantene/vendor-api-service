package com.nextgen.gameaggregator.vendor.pinnacle.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionsPlayerInfoDto {
    @JsonProperty("LoginId")
    private String loginId;

    @JsonProperty("UserCode")
    private String userCode;
}
