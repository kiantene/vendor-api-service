package com.nextgen.gameaggregator.vendor.joker.api.bet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.enums.WinType;
import com.nextgen.gameaggregator.operator.wallet.settled.UnsettledResultSettledData;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BetDto implements UnsettledResultSettledData {

    private String appid;

    private String hash;

    private String id;

    private BigDecimal amount;

    private String username;

    private Long timestamp;

    private String gamecode;

    private String roundid;

    @Override
    public String getExternalTransactionId() {
        return this.id;
    }

    @Override
    public String getVendorBetId() {
        return this.id;
    }

    @Override
    public String getRoundId() {
        return this.roundid;
    }

    @Override
    public String getGameId() {
        return this.gamecode;
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.amount;
    }

    @Override
    public BigDecimal getWinAmount() {
        return BigDecimal.valueOf(0);
    }

    @Override
    public BigDecimal getWinLoss() {
        return getBetAmount().negate();
    }

    @Override
    public BigDecimal getVendorWinLoss() {
        return getBetAmount().negate();
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return getBetAmount();
    }

    @Override
    public WinType getResultType() { return WinType.LOSE; }

    @Override
    public BigDecimal getRefundAmount() {
        return BigDecimal.valueOf(0);
    }

    @Override
    public Long getVendorBetTime() {
        return getTimestamp();
    }

    @Override
    public Long getResultTime() {
        return getTimestamp();
    }

    @Override
    public Long getVendorSettleTime() {
        return getTimestamp();
    }

    @Override
    public BigDecimal getJackpotAmount() { return BigDecimal.ZERO;}

    @Override
    public Integer getIsCancelled() {
        return 0;
    }

    @Override
    public Integer getIsFreespin() {
        return 0;
    }
}
