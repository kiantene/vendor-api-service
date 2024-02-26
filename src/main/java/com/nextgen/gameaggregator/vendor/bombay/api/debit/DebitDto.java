package com.nextgen.gameaggregator.vendor.bombay.api.debit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DebitDto implements BetResultData {
    @NotBlank
    private String transaction_uuid;

    @NotBlank
    private String token;

    @NotBlank
    private String round;

    @NotBlank
    private String request_uuid;

    @NotNull
    private String game_id;

    @NotBlank
    private String currency;

    @NotNull
    private Integer amount;

    @Override
    public String getExternalTransactionId() {
        return this.transaction_uuid;
    }

    @Override
    public String getVendorBetId() {
        return this.transaction_uuid;
    }

    @Override
    public String getRoundId() {
        return this.round;
    }

    @Override
    public String getGameId() {
        return this.game_id;
    }

    @Override
    public BigDecimal getBetAmount() {
        return new BigDecimal(this.amount);
    }

    @Override
    public BigDecimal getWinAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinLoss() {
        return null;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return new BigDecimal(this.amount);
    }

    @Override
    public Long getVendorBetTime() {
        return System.currentTimeMillis(); //unix timestamp with millisecond
    }

    @Override
    public Long getResultTime() {
        return null;
    }

    @Override
    public Long getVendorSettleTime() {
        return null;
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return null;
    }

    @Override
    public Integer getIsFreespin() {
        return 0;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.UNSETTLED;
    }
}
