package com.nextgen.gameaggregator.vendor.ezugi.api.v2.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetRequest {
    @NotBlank
    @Size(min = 1, max = 50)
    @JsonProperty("uid")
    private String uid;

    @NotBlank
    @Size(min = 1, max = 100)
    @JsonProperty("transactionId")
    private String transactionId;

    @NotNull
    @Digits(integer = 18, fraction = 0)
    @JsonProperty("roundId")
    private BigInteger roundId;

    @NotNull
    @Digits(integer = 4, fraction = 0)
    @JsonProperty("gameId")
    private Integer gameId;

    @NotNull
    @JsonProperty("tableId")
    private Integer tableId;

    @NotNull
    @PositiveOrZero(message = "Negative amount")
    @JsonProperty("debitAmount")
    private BigDecimal debitAmount;

    @NotNull
    @Digits(integer = 4, fraction = 0)
    @JsonProperty("betTypeID")
    private Integer betTypeID;

    @NotBlank
    @Size(min = 1, max = 5)
    @JsonProperty("currency")
    private String currency;

    @NotNull
    @JsonProperty("operatorId")
    private Integer operatorId;

    @NotBlank
    @Size(min = 1, max = 250)
    @JsonProperty("token")
    private String token;

    @JsonProperty("platformId")
    private Integer platformId;

    @NotNull
    @Digits(integer = 13, fraction = 0)
    @JsonProperty("timestamp")
    private Long timestamp;
}
