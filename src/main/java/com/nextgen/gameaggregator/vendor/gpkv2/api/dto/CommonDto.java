package com.nextgen.gameaggregator.vendor.gpkv2.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonDto {

    @NotBlank
    @JsonProperty("action")
    private String action;
    @NotBlank
    @JsonProperty("currency")
    private String currency;
    @NotBlank
    @JsonProperty("operator_player_id")
    private String operatorPlayerId;
    @NotBlank
    @JsonProperty("session_token")
    private String sessionToken;
    @NotNull
    @JsonProperty("provider")
    private Integer provider;
    @NotBlank
    @JsonProperty("timestamp")
    private String timestamp;
}