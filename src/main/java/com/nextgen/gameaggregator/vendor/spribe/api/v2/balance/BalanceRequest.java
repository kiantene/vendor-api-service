package com.nextgen.gameaggregator.vendor.spribe.api.v2.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BalanceRequest {
    @NotBlank
    private String userId;

    @NotBlank
    private String sessionToken;

    @NotBlank
    private String currency;
}
