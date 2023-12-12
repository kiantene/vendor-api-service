package com.nextgen.gameaggregator.vendor.saba.api.parlayconfirmbet;

import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.sport.entity.SportBetResultData;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ConfirmBetParlayTxnsDto implements SportBetResultData {
    private String refId;
    private String txId;
    private String licenseeTxId;
    private BigDecimal actualAmount;
    private Boolean isOddsChanged;
    private BigDecimal creditAmount;
    private BigDecimal debitAmount;
    private String winlostDate;
    private BigDecimal odds;

    private String userId;

    @Override
    public String getExternalTransactionId() {
        return this.refId;
    }

    @Override
    public String getVendorBetId() {
        return this.txId;
    }

    @Override
    public String getRoundId() {
        return this.refId;
    }

    @Override
    public String getGameId() {
        return null;
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.actualAmount;
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
    public BigDecimal getJackpotAmount() {
        return null;
    }

    @Override
    public Integer getIsFreespin() {
        return 0;
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
        return null;
    }
}
