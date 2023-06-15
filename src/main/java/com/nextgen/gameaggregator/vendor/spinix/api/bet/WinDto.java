package com.nextgen.gameaggregator.vendor.spinix.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class WinDto implements BetResultData {

    // Request Id sent by vendor
    private String reqId;

    // Id of the round
    private String roundId;

    // Id of the win transaction
    private String id;

    // Amount of the win transaction
    private BigDecimal amount;

    // Result type of the bet record
    private ResultType resultType;

    // Valid turnover of the bet record
    private BigDecimal validTurnover;

    // Id of vendor's game
    private String gameId;

    // Time of win transaction
    private Long timestamp;

    // Bet status of the win record
    private BetStatus betStatus;

    // Whether the transaction is a free spin
    private Integer freeSpin;

    // External transaction id of the bet record
    private String externalTransactionId;

    @Override
    public String getVendorBetId() {
        return this.roundId;
    }

    @Override
    public BigDecimal getBetAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.amount;
    }

    @Override
    public BigDecimal getWinLoss() {
        if (this.getBetStatus() == BetStatus.UNSETTLED) {
            // return win loss as 0 if bet still not settled
            return BigDecimal.ZERO;
        }
        return this.amount;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return BigDecimal.ZERO;
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
    public Integer getIsFreespin() {
        return this.freeSpin;
    }

    @Override
    public BetStatus getBetStatus() {
        return this.betStatus;
    }
}

