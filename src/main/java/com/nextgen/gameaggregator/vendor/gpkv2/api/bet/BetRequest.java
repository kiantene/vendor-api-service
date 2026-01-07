package com.nextgen.gameaggregator.vendor.gpkv2.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.gpkv2.api.dto.CommonDto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetRequest extends CommonDto {

    @NotBlank
    @JsonProperty("transaction_id")
    private String transactionId;

    @NotBlank
    @JsonProperty("round_id")
    private String roundId;

    @NotNull
    @Digits(integer = 20, fraction = 4)
    @DecimalMin(value = "0.0")
    @JsonProperty("amount")
    private BigDecimal amount;

    @NotBlank
    @JsonProperty("game_token")
    private String gameToken;

    @JsonProperty("finished")
    private Boolean finished;

}
