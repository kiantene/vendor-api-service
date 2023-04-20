package com.nextgen.gameaggregator.vendor.spinix.api.bet;

import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BetWinDto implements BetResultData {
    private String reqId;
    private String roundId;
    private String id;
    private BigDecimal betAmount;
    private BigDecimal winAmount;
    private ResultType resultType;
    private BigDecimal validTurnover;
    private String gameId;
    private Long timestamp;

    @Override
    public String getExternalTransactionId() {
        return this.roundId;
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
    public BigDecimal getEffectiveTurnover() {
        return this.validTurnover;
    }

    @Override
    public BigDecimal getRefundAmount() {
        return BigDecimal.ZERO;
    }

//    @Override
//    public ResultType getResultType() {
//        this.resultType = ResultType.WIN;
//
//        if(this.betAmount.compareTo(BigDecimal.ZERO) > 0 && this.winAmount.compareTo(BigDecimal.ZERO) == 0) {
//            this.resultType = ResultType.LOSE;
//        }
//        return this.resultType;
//    }

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
    public Integer getIsFreespin() {
        return 0;
    }

    /**
     * @return
     */
    @Override
    public BetStatus getBetStatus() {
        return BetStatus.SETTLED;
    }
}

