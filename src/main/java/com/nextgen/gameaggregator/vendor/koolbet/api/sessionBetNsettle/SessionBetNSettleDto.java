package com.nextgen.gameaggregator.vendor.koolbet.api.sessionBetNsettle;

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
public class SessionBetNSettleDto extends CommonDto implements BetResultData {
    @NotNull
    @Positive
    private double betAmount;

    @NotNull
    @NotBlank
    private String currency;

    @NotNull
    @Positive
    private int game;

    private String platform;

    private int preserve;

    @NotNull
    @Positive
    private long round;

    @NotNull
    @Positive
    private int sessionId;

    private double turnover;

    @NotNull
    @Positive
    private int type;

    @NotNull
    @NotBlank
    private String userId;

    @NotNull
    @Positive
    private long wagersTime;

    private double winloseAmount;

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
        return String.valueOf(this.sessionId);
    }

    @Override
    public String getGameId() {
        return String.valueOf(this.game);
    }

    @Override
    public BigDecimal getBetAmount() {
        return type == 2 ? null : BigDecimal.valueOf(this.betAmount);
    }

    @Override
    public BigDecimal getWinAmount() {
        return type == 1 ? null : BigDecimal.valueOf(this.winloseAmount);
    }

    @Override
    public BigDecimal getWinLoss() {
        return null;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return BigDecimal.valueOf(this.turnover);
    }

    @Override
    public Long getVendorBetTime() {
        return type == 2 ? null : this.wagersTime;
    }

    @Override
    public Long getResultTime() {
        return System.currentTimeMillis();
    }

    @Override
    public Long getVendorSettleTime() {
        return type == 1 ? null : this.wagersTime;
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
        return type == 1 ? BetStatus.UNSETTLED : BetStatus.SETTLED;
    }

}
