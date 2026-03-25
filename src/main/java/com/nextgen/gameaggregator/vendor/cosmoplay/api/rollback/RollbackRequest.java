package com.nextgen.gameaggregator.vendor.cosmoplay.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollbackRequest {
    @NotBlank
    @JsonProperty("SpinID")
    private String spinID;

    @NotBlank
    @JsonProperty("RoundID")
    private String roundID;

    @NotBlank
    @JsonProperty("PlayerID")
    private String playerID;

    @NotBlank
    @JsonProperty("GameID")
    private String gameID;
}
