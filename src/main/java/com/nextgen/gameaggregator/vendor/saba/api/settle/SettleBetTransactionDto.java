package com.nextgen.gameaggregator.vendor.saba.api.settle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.sport.entity.SportBetResultData;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SettleBetTransactionDto implements SportBetResultData {
    private String userId;
    private String refId;
    private Long txId;
    private String updateTime;
    private String winlostDate;
//    private String status;
    private BigDecimal payout;
    private BigDecimal creditAmount;
    private BigDecimal debitAmount;
    private String extraStatus;

    @Override
    public String getExternalTransactionId() {
        return this.getRefId();
    }

    @Override
    public String getVendorBetId() {
        return this.getTxId().toString();
    }

    @Override
    public String getRoundId() {
        return this.getTxId().toString();
    }

    @Override
    public String getGameId() {
        return "saba";
    }

    @Override
    public BigDecimal getBetAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.getPayout();
    }

    @Override
    public BigDecimal getWinLoss() {
        return null;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return null;
    }

    @Override
    public Long getVendorBetTime() {
        return null;
    }

    @Override
    public Long getResultTime() {
        return System.currentTimeMillis();
    }

    @Override
    public Long getVendorSettleTime() {
        return System.currentTimeMillis();
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
    public BigDecimal getActualBetAmount() {
        return null;
    }

    @Override
    public BigDecimal getOdds() {
        return null;
    }

    @Override
    public Integer getOddTypeId() {
        return null;
    }
}
