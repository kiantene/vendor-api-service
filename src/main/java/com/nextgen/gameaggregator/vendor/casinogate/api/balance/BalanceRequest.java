package com.nextgen.gameaggregator.vendor.casinogate.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceRequest {
    @NotBlank
    private String token;
}
