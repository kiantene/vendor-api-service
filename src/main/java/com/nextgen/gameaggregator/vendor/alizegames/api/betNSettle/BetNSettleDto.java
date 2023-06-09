package com.nextgen.gameaggregator.vendor.alizegames.api.betNSettle;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetNSettleDto implements BetResultData {

    private String traceId;
    private String betId;
    private String roundId;
    private String token;
    private String username;
    private String currency;
    private String operatorId;

    private BigDecimal stake;
    private BigDecimal payout;
    private BigDecimal winloss;
    private String gameCode;
    private String ip;
//    private String info;
//    private String result;
//    private String hits;
    private Long betTime;
    private Long processedTime;
    private Long timestamp;

    @Override
    public String getExternalTransactionId() {
        return this.betId;
    }

    @Override
    public String getVendorBetId() {
        return this.betId;
    }

    @Override
    public String getRoundId() {
        return this.roundId;
    }

    @Override
    public String getGameId() {
        return this.gameCode;
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.stake;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.payout;
    }

    @Override
    public BigDecimal getWinLoss() {
        return this.winloss;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return null;
    }

    @Override
    public Long getVendorBetTime() {
        return this.betTime;
    }

    @Override
    public Long getResultTime() {
        return System.currentTimeMillis();
    }

    @Override
    public Long getVendorSettleTime() {
        return this.processedTime;
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
}
