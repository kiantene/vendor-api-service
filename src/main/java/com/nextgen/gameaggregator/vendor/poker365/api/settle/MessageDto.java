package com.nextgen.gameaggregator.vendor.poker365.api.settle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageDto {

    @Valid
    @JsonProperty("message")
    private TransactionsDto transactionsDto;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("userId")
    private String userId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("currency")
    private String currency;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("gameNumber")
    private String gameNumber;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("txId")
    private String txId;

    @NotBlank
    @Size(max = 255)
    @JsonProperty("gameId")
    private String gameId;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    @DecimalMin(value = "0.0")
    @JsonProperty("profit")
    private BigDecimal profit;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    @DecimalMin(value = "0.0")
    @JsonProperty("bonus")
    private BigDecimal bonus;
}
