package com.nextgen.gameaggregator.vendor.saba.api.confirmbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.sport.entity.SportBetResultData;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfirmBetDto implements SportBetResultData {
    private String action;
    private String operationId;
    private String userId;
    private String updateTime;
    private String transactionTime;
    private List<ConfirmBetTransactionDto> txns;

    @Override
    public String getExternalTransactionId() {
        return this.getTxns().get(0).getRefId();
    }

    @Override
    public String getVendorBetId() {
        return this.getTxns().get(0).getTxId().toString();
    }

    @Override
    public String getRoundId() {
        return this.getTxns().get(0).getTxId().toString();
    }

    @Override
    public String getGameId() {
        return null;
    }

    @Override
    public BigDecimal getBetAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        return null;
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
        return System.currentTimeMillis();
    }

    @Override
    public Long getResultTime() {
        return null;
    }

    @Override
    public Long getVendorSettleTime() {
        return null;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.UNSETTLED;
    }

    @Override
    public String getVendorPlayerUsername() {
        return this.userId;
    }

    @Override
    public BigDecimal getNewBetAmount() {
        return this.getTxns().get(0).getActualAmount();
    }
}
