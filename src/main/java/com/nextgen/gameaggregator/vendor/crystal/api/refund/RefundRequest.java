package com.nextgen.gameaggregator.vendor.crystal.api.refund;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefundRequest {

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
