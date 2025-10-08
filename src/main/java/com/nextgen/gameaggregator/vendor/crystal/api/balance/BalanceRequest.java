package com.nextgen.gameaggregator.vendor.crystal.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceRequest {
    @NotBlank
    @Size(max = 255)
    @JsonProperty("currencyCode")
    private String currencyCode;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("playerId")
    private String playerId;

}
