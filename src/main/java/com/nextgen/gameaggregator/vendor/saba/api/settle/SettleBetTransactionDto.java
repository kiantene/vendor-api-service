package com.nextgen.gameaggregator.vendor.saba.api.settle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.sport.settle.SportBetResultData;
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
    private String operationId;

    @Override
    public String getExternalTransactionId() {
        return operationId;
    }

    @Override
    public String getVendorBetId() {
        return this.getTxId().toString();
    }

    @Override
    public String getRoundId() {
        return this.getRefId();
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
    public BetStatus getBetStatus() {
        return BetStatus.SETTLED;
    }

    @Override
    public Integer getBetType() {
        return null;
    }

    @Override
    public String getVendorPlayerUsername() {
        return this.userId;
    }

    @Override
    public BigDecimal getNewBetAmount() {
        return null;
    }
}
