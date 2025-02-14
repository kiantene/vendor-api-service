package com.nextgen.gameaggregator.vendor.koolbet.api.sessionBetNsettle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.koolbet.api.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.koolbet.constant.Formats;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionBetNSettleDto extends CommonDto implements BetResultData {

    @NotBlank
    @Size(max = 5)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String currency;

    @NotNull
    private Integer game;

    @NotNull
    @PositiveOrZero
    private Long round;

    @NotNull
    @PositiveOrZero
    private Long wagersTime;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 20, fraction = 2)
    private BigDecimal betAmount;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 20, fraction = 2)
    private BigDecimal winloseAmount;

    @NotNull
    @Positive
    private BigInteger sessionId;

    @NotNull
    @Positive
    private Integer type;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 20, fraction = 2)
    private BigInteger preserve;


    @Override
    public String getExternalTransactionId() {
        return String.valueOf(this.round);
    }

    @Override
    public String getVendorBetId() {
        return String.valueOf(this.round);
    }

    @Override
    public String getRoundId() {
        return String.valueOf(this.sessionId);
    }

    @Override
    public String getGameId() {
        return String.valueOf(this.game);
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.betAmount;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.winloseAmount;
    }

    @Override
    public BigDecimal getWinLoss() {
        return this.betAmount.negate();
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return this.betAmount;
    }

    @Override
    public Long getVendorBetTime() {
        return this.wagersTime;
    }

    @Override
    public Long getResultTime() {
        return System.currentTimeMillis();
    }

    @Override
    public Long getVendorSettleTime() {
        return type == Formats.SESSION_BET_TYPE_BET ? null : this.wagersTime;
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
        return type == Formats.SESSION_BET_TYPE_BET ? BetStatus.UNSETTLED : BetStatus.SETTLED;
    }

}
