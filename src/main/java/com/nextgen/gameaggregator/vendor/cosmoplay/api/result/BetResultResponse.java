package com.nextgen.gameaggregator.vendor.cosmoplay.api.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetResultResponse {
    @NotNull
    @JsonProperty("Balance")
    private Long balance;
}
