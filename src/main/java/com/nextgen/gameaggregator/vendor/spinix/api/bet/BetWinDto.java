package com.nextgen.gameaggregator.vendor.spinix.api.bet;

import com.nextgen.gameaggregator.enums.WinType;
import com.nextgen.gameaggregator.operator.wallet.settled.UnsettledResultSettledData;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BetWinDto implements UnsettledResultSettledData {
    private String reqId;
    private String roundId;
    private String id;
    private BigDecimal betAmount;
    private BigDecimal winAmount;
    private WinType winType;
    private BigDecimal validTurnover;
    private String gameId;
    private Long timestamp;

    @Override
    public String getExternalTransactionId() {
        return this.reqId;
    }

    @Override
    public String getVendorBetId() {
        return this.roundId;
    }

    @Override
    public BigDecimal getBetAmount() { return this.betAmount; }

    @Override
    public BigDecimal getWinAmount() {
        return this.winAmount;
    }

    @Override
    public BigDecimal getWinLoss() {
        return this.winAmount.subtract(this.betAmount);
    }

    @Override
    public BigDecimal getVendorWinLoss() {
        return this.winAmount.subtract(this.betAmount);
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return this.validTurnover;
    }

    @Override
    public BigDecimal getRefundAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public WinType getResultType() {
        this.winType = WinType.WIN;

        if(this.betAmount.compareTo(BigDecimal.ZERO) > 0 && this.winAmount.compareTo(BigDecimal.ZERO) == 0) {
            this.winType = WinType.LOSE;
        }
        return this.winType;
    }

    @Override
    public Long getVendorBetTime() {
        return this.timestamp;
    }

    @Override
    public Long getResultTime() {
        return this.timestamp;
    }

    @Override
    public Long getVendorSettleTime() {
        return this.timestamp;
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public Integer getIsCancelled() {
        return 0;
    }

    @Override
    public Integer getIsFreespin() {
        return 0;
    }
}

