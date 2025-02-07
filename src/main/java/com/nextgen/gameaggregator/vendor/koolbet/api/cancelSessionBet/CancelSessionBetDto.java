package com.nextgen.gameaggregator.vendor.koolbet.api.cancelSessionBet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelSessionBetDto implements RollbackData {

    @NotBlank
    public String reqId;

    @NotNull
    @NotBlank
    private String currency;

    @NotNull
    @Positive
    private Integer game;

    @NotNull
    @Positive
    private long round;

    @NotNull
    @Positive
    private BigDecimal betAmount;

    @NotNull
    private BigDecimal winloseAmount;

    @NotNull
    @NotBlank
    private String userId;

    @NotNull
    @Positive
    private BigInteger sessionId;

    @NotNull
    @Positive
    private Integer type;

    @Override
    public String getRollbackId() {
        return String.valueOf(this.round);
    }

    @Override
    public Long getVendorSettledTime() {
        return System.currentTimeMillis();
    }

    @Override
    public String getRoundId() {
        return null;
    }

}
