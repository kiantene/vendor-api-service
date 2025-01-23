package com.nextgen.gameaggregator.vendor.koolbet.api.reward;

import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.vendor.koolbet.api.dto.CommonDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RewardDto extends CommonDto implements BetResultData {


    @NotBlank
    private String currency;

    @NotNull
    private Integer game;

    @NotNull
    @Positive
    private Integer activityId;

    @NotNull
    private long orderId;

    @NotNull
    private long wagersTime;

    @NotNull
    @PositiveOrZero
    private BigDecimal amount;

    //Optional
    private String platform;

    @Override
    public String getExternalTransactionId() {
        return String.valueOf(this.orderId);
    }

    @Override
    public String getVendorBetId() {
        return String.valueOf(this.orderId);
    }

    @Override
    public String getRoundId() {
        return String.valueOf(this.orderId);
    }

    @Override
    public String getGameId() {
        return String.valueOf(this.game);
    }

    @Override
    public BigDecimal getBetAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.amount;
    }

    @Override
    public BigDecimal getWinLoss() {
        return this.amount;
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
        return this.wagersTime;
    }

    @Override
    public Long getVendorSettleTime() {
        return this.wagersTime;
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return null;
    }

    @Override
    public Integer getIsFreespin() {
        return null;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.SETTLED;
    }
}
