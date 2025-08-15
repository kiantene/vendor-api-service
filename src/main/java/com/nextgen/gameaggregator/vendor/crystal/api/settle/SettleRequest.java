package com.nextgen.gameaggregator.vendor.crystal.api.settle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@JsonDeserialize(builder = SettleRequest.SettleRequestBuilder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SettleRequest {
    @NotBlank
    @Size(max = 255)
    @JsonProperty("currencyCode")
    private String currencyCode;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("playerId")
    private String playerId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("roundId")
    private String roundId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("transactionId")
    private String transactionId;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    @DecimalMin(value = "0.0")
    @JsonProperty("amount")
    private BigDecimal amount;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("gameCode")
    private String gameCode;
}
