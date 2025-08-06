package com.nextgen.gameaggregator.vendor.aviatorstudio.api.cashout;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@JsonDeserialize(builder = CashOutRequest.CashOutRequestBuilder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CashOutRequest {
    @NotNull
    @Digits(integer = 20, fraction = 8)
    private BigDecimal amount;

    @NotBlank
    @Size(max = 10)
    private String gameId;

    @NotBlank
    @Size(max = 255)
    private String transactionId;

    @NotBlank
    @Size(max = 255)
    private String roundId;

    @NotBlank
    @Size(max = 5)
    private String currency;

    @NotBlank
    @Size(max = 255)
    private String sessionId;
}
