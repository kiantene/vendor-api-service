package com.nextgen.gameaggregator.vendor.db.api.transfer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.db.constant.TradeType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransferDto implements BetResultData {

    @NotBlank
    @Size(max = 50)
    private String memberId;

    @NotBlank
    @Size(max = 255)
    private String betId;

    @NotBlank
    @Size(max = 255)
    private String tradeId;

    @NotNull
    private Integer tradeType;

    @Size(max = 5)
    private String currency;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    private BigDecimal tradeAmount;

    @Digits(integer = 20, fraction = 0)
    private Integer gameId;

    @Override
    public String getExternalTransactionId() {
        return this.tradeId;
    }

    @Override
    public String getVendorBetId() {
        return this.betId;
    }

    @Override
    public String getRoundId() {
        return this.betId;
    }

    @Override
    public BigDecimal getBetAmount() {
        if (this.tradeType == TradeType.BET) {
            return this.tradeAmount;
        }
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        if (this.tradeType == TradeType.PAYOUT) {
            return this.tradeAmount;
        }
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
        return System.currentTimeMillis();
    }

    @Override
    public String getGameId() {
        return this.gameId.toString();

    }

    @Override
    public Long getVendorSettleTime() {
        return System.currentTimeMillis();
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

        if (this.tradeType == TradeType.BET) {
            return BetStatus.UNSETTLED;
        }
        return BetStatus.SETTLED;

    }

}
