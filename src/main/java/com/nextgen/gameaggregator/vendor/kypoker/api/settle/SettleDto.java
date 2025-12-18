package com.nextgen.gameaggregator.vendor.kypoker.api.settle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.core.RequestIdempotency;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.kypoker.constant.RoomCode;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class SettleDto implements BetResultData, RequestIdempotency {

    @NotBlank
    @Size(min = 1, max = 36)
    private String s;

    @NotBlank
    @Size(min = 1, max = 36)
    private String account;

    @NotBlank
    private String orderId;

    @NotBlank
    @Size(min = 1, max = 36)
    private String gameNo;

    @NotNull
    @Digits(integer = 5, fraction = 0)
    private Integer kindId;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    private BigDecimal money;

    @NotBlank
    @Size(min = 1, max = 36)
    private String currency;

    @NotBlank
    @Size(min = 1, max = 36)
    private String gameId;

    @NotNull
    @Digits(integer = 1, fraction = 0)
    private Integer roomMode;

    @NotNull
    @Digits(integer = 35, fraction = 0)
    private Integer betCount;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    private BigDecimal totalBet;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    private BigDecimal validBet;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    private BigDecimal totalWithdraw;

    @NotNull
    @Digits(integer = 20, fraction = 8)
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
        //only use for debit/credit scenario
        if (this.roomMode == RoomCode.MATCHING.code) {
            return this.validBet.add(this.totalWithdraw);
        }
        return this.money;
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

    @Override
    public String getTransactionId() {
        return getExternalTransactionId();
    }

    @Override
    public String getVendorPlayerUsername() {
        return this.account;
    }
}
