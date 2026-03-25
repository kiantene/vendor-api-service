package com.nextgen.gameaggregator.vendor.cockfight6.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceRequest {
    @JsonProperty("external_player_id")
    @NotBlank
    private String playerName;
}
