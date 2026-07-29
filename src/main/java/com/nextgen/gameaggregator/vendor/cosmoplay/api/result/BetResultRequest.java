package com.nextgen.gameaggregator.vendor.cosmoplay.api.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.nextgen.gameaggregator.vendor.cosmoplay.util.CustomBooleanDeserializer;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigInteger;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetResultRequest {
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
    @JsonDeserialize(using = CustomBooleanDeserializer.class)
    @JsonProperty("IsRoundEnd")
    private Boolean isRoundEnd;

    @NotNull
    @Digits(integer = 20, fraction = 0)
    @PositiveOrZero(message = "must be greater than or equal to 0")
    @JsonProperty("WinAmount")
    private BigInteger winAmount;
}
