package com.nextgen.gameaggregator.vendor.kypoker.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.core.RequestIdempotency;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class BetDto implements BetResultData, RequestIdempotency {

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

    @NotBlank
    @Size(min = 1, max = 36)
    private String gameId;

    @NotNull
    @Digits(integer = 5, fraction = 0)
    private Integer kindId;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    private BigDecimal money;

    @NotBlank
    @Size(min = 1, max = 36)
    private String currency;

    private Long timeStamp;

    @NotNull
    @Digits(integer = 1, fraction = 0)
    private Integer roomMode;

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
        return this.timeStamp;
    }

    @Override
    public Long getResultTime() {
        return null;
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
        return BetStatus.UNSETTLED;
    }

    @Override
    public String getTransactionId() {
        return this.orderId;
    }

    @Override
    public String getVendorPlayerUsername() {
        return this.account;
    }
}
