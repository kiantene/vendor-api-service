package com.nextgen.gameaggregator.scheduler.betaction;

import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GeneralSettleDto implements BetResultData {

    private String externalTransactionId;
    private String vendorBetId;
    private String roundId;
    private String gameId;
    private BigDecimal betAmount;
    private BigDecimal winAmount;
    private BigDecimal winLoss;
    private BigDecimal effectiveTurnover;
    private Long vendorBetTime;
    private Long resultTime;
    private Long vendorSettleTime;
    private BigDecimal jackpotAmount;
    private Integer isFreeSpin;
    private BetStatus betStatus;
    private boolean shouldSettleByBet;

    @Override
    public String getExternalTransactionId() {
        return this.externalTransactionId;
    }

    @Override
    public String getVendorBetId() {
        return this.vendorBetId;
    }

    @Override
    public String getRoundId() {
        return this.roundId;
    }

    @Override
    public String getGameId() {
        return this.gameId;
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.betAmount;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.winAmount;
    }

    @Override
    public BigDecimal getWinLoss() {
        return this.winLoss;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return this.effectiveTurnover;
    }

    @Override
    public Long getVendorBetTime() {
        return this.vendorBetTime;
    }

    @Override
    public Long getResultTime() {
        return this.resultTime;
    }

    @Override
    public Long getVendorSettleTime() {
        return this.vendorSettleTime;
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return this.jackpotAmount;
    }

    @Override
    public Integer getIsFreespin() {
        return this.isFreeSpin;
    }

    @Override
    public BetStatus getBetStatus() {
        return this.betStatus;
    }

    @Override
    public boolean getShouldSettleByBet(){return this.shouldSettleByBet;}
}
