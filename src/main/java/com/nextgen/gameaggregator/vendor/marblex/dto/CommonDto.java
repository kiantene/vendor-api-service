package com.nextgen.gameaggregator.vendor.marblex.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonDto {
    @JsonProperty("TraceID")
    private String traceId;
    @JsonProperty("PlayerID")
    private String playerId;
    @JsonProperty("Currency")
    private String currency;
}
