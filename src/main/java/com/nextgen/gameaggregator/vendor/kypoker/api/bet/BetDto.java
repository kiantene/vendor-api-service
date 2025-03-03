package com.nextgen.gameaggregator.vendor.kypoker.api.bet;

import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BetDto implements BetResultData {

    private Integer s;

    private String account;

    private String orderId;

    private String gameNo;

    private Integer gameId;

    private Integer kindId;

    private BigDecimal money;

    private String currency;

    @Override
    public String getExternalTransactionId() {
        return this.orderId;
    }

    @Override
    public String getVendorBetId() {
        return this.orderId;
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
        return Long.valueOf(0);
    }

    @Override
    public Long getResultTime() {
        return Long.valueOf(0);
    }

    @Override
    public Long getVendorSettleTime() {
        return Long.valueOf(0);
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
