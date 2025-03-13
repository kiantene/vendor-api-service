package com.nextgen.gameaggregator.vendor.kypoker.api.settle;

import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SettleDto implements BetResultData {

    private Integer s;

    private String account;

    private String orderId;

    private String gameNo;

    private String gameId;

    private Integer kindId;

    private BigDecimal money;

    private String currency;

    @Override
    public String getExternalTransactionId() {
        return this.orderId;
    }

    @Override
    public String getVendorBetId() {
        return "";
    }

    @Override
    public String getRoundId() {
        return this.gameNo;
    }

    @Override
    public String getGameId() {
        return String.valueOf(this.gameId);
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.money;
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
        return 0L;
    }

    @Override
    public Long getResultTime() {
        return 0L;
    }

    @Override
    public Long getVendorSettleTime() {
        return 0L;
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
        return null;
    }
}
