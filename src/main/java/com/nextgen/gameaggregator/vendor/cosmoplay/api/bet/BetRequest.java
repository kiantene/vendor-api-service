package com.nextgen.gameaggregator.vendor.cosmoplay.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigInteger;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetRequest {
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

    @NotNull
    @Digits(integer = 20, fraction = 0)
    @PositiveOrZero(message = "must be greater than or equal to 0")
    @JsonProperty("BetAmount")
    // The amount of money the player bet in given spins, multiplied by 100 to represent
    // decimals if the decimal place is 2 (e.g., 100.00 is represented as 10000).
    private BigInteger betAmount;
}
