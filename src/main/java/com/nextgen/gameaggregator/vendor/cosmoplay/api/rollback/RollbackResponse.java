package com.nextgen.gameaggregator.vendor.cosmoplay.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollbackResponse {
    @NotNull
    @JsonProperty("Balance")
    private Long balance;
}
