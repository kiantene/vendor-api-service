package com.nextgen.gameaggregator.vendor.spribe.api.v2.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BetRequest {
    @NotBlank
    private String userId;

    @NotBlank
    private String sessionToken;

    @NotBlank
    private String currency;

    @NotNull
    @PositiveOrZero
    private BigDecimal amount;

    @NotBlank
    private String game;

    @NotBlank
    private String actionId;

    @NotBlank
    private String action;

    @NotBlank
    private String provider;

    @NotBlank
    private String providerTxId;

    private String platform;
}
