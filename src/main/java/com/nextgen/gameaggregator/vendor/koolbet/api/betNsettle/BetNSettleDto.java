package com.nextgen.gameaggregator.vendor.koolbet.api.betNsettle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.koolbet.api.dto.CommonDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetNSettleDto extends CommonDto implements BetResultData {
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
    private long wagersTime;

    @NotNull
    @Positive
    private double betAmount;

    @NotNull
    private double winloseAmount;

    private String platform;

    @Override
    public String getExternalTransactionId() {
        return String.valueOf(this.round);
    }

    @Override
    public String getVendorBetId() {
        return String.valueOf(this.round);
    }

    @Override
    public String getRoundId() {
        return String.valueOf(this.round);
    }

    @Override
    public String getGameId() {
        return String.valueOf(this.game);
    }

    @Override
    public BigDecimal getBetAmount() {
        return BigDecimal.valueOf(this.betAmount);
    }

    @Override
    public BigDecimal getWinAmount() {
        return BigDecimal.valueOf(this.winloseAmount);
    }

    @Override
    public BigDecimal getWinLoss() {
        return null;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return BigDecimal.valueOf(this.betAmount);
    }

    @Override
    public Long getVendorBetTime() {
        return this.wagersTime;
    }

    @Override
    public Long getResultTime() {
        return System.currentTimeMillis();
    }

    @Override
    public Long getVendorSettleTime() {
        return this.wagersTime;
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public Integer getIsFreespin() {
        return 0;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.SETTLED;
    }

    @Override
    public boolean getShouldSettleByBet() {
        return true;
    }
}
