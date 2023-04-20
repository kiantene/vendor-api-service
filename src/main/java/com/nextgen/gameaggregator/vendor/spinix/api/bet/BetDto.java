package com.nextgen.gameaggregator.vendor.spinix.api.bet;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BetDto implements BetResultData {
    private String reqId;
    private String roundId;
    private String id;
    private BigDecimal amount;
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
        return this.id;
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.amount;
    }

    @Override
    public BigDecimal getWinAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getWinLoss() {
        return getBetAmount().negate();
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
//        return ResultType.LOSE;
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
