package com.nextgen.gameaggregator.vendor.kypoker.api.settle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import jnr.ffi.annotations.In;
import lombok.Data;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class SettleDto implements BetResultData {

    @JsonProperty("s")
    private String s;

    private String account;

    private String orderId;

    private String gameNo;

    private Integer kindId;

    private BigDecimal money;

    private String currency;

    private String gameId;

    private Integer roomMode;

    private Integer betCount;

    private BigDecimal totalBet;

    private BigDecimal validBet;

    private BigDecimal totalWithdraw;

    private BigDecimal revenue;

    private Long timeStamp;

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
        return String.valueOf(this.kindId);
    }

    @Override
    public BigDecimal getBetAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.totalWithdraw.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : this.totalWithdraw;
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
        return this.timeStamp;
    }

    @Override
    public Long getVendorSettleTime() {
        return this.timeStamp;
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
        return BetStatus.SETTLED;
    }
}
