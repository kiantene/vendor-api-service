package com.nextgen.gameaggregator.vendor.koolbet.api.reward;

import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.koolbet.dto.CommonDto;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RewardDto extends CommonDto implements BetResultData {


    @NotBlank
    @Size(max = 5)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String currency;

    @NotNull
    private Integer game;

    @NotNull
    @Positive
    private Integer activityId;

    @NotNull
    @PositiveOrZero
    private Long orderId;

    @NotNull
    @PositiveOrZero
    private Long wagersTime;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 20, fraction = 2)
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
