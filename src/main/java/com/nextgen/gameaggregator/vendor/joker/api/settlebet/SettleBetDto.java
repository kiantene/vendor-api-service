package com.nextgen.gameaggregator.vendor.joker.api.settlebet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.enums.WinType;
import com.nextgen.gameaggregator.operator.wallet.settled.UnsettledResultSettledData;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SettleBetDto implements UnsettledResultSettledData {

    private String appid;

    private String hash;

    private String id;

    private BigDecimal amount;

    private String username;

    private Long timestamp;

    private String gamecode;

    private String roundid;

    private String description;

    private String type;
    private WinType resultType;

    private BigDecimal winLoss;
    private BigDecimal vendorWinLoss;
    private BigDecimal effectiveTurnover;

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
        return BigDecimal.valueOf(0);
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.amount;
    }

    @Override
    public BigDecimal getWinLoss() {
        return (this.amount.subtract(this.getBetAmount()));
    }

    @Override
    public BigDecimal getVendorWinLoss()  {
        return (this.amount.subtract(this.getBetAmount()));
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return BigDecimal.valueOf(0);
    }

    @Override
    public WinType getResultType() {
        return (this.getWinAmount().compareTo(BigDecimal.ZERO) > 0)?WinType.WIN:WinType.LOSE;
    }

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
