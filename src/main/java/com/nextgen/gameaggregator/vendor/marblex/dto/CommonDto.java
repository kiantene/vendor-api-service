package com.nextgen.gameaggregator.vendor.marblex.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonDto {
    @JsonProperty("TraceID")
    private String traceId;
    @NotBlank
    @JsonProperty("PlayerID")
    private String playerId;
    @NotBlank
    @JsonProperty("Currency")
    private String currency;
}
