package com.nextgen.gameaggregator.vendor.koolbet.api.sessionbetnsettle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.koolbet.constant.Formats;
import com.nextgen.gameaggregator.vendor.koolbet.dto.CommonDto;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionBetNSettleDto extends CommonDto implements BetResultData {

    @NotBlank
    @Size(max = 5)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String currency;

    @NotNull
    @Digits(integer = 20, fraction = 0)
    private BigDecimal game;

    @NotNull
    @PositiveOrZero
    private BigInteger round;
    
    private List<String> betOrder;

    @NotNull
    @PositiveOrZero
    private Long wagersTime;

    @NotNull
    @Positive
    private BigDecimal betAmount;

    @NotNull
    @PositiveOrZero
    private BigDecimal winloseAmount;

    @NotNull
    @PositiveOrZero
    private BigInteger sessionId;

    @NotNull
    @Positive
    private Integer type;

    @NotNull
    @PositiveOrZero
    private BigDecimal preserve;


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
        return this.wagersTime * 1000;
    }

    @Override
    public Long getResultTime() {
        return System.currentTimeMillis();
    }

    @Override
    public Long getVendorSettleTime() {
        return type == Formats.SESSION_BET_TYPE_BET ? null : this.wagersTime * 1000;
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

    @Override
    public boolean getShouldSettleByBet() {
        
        if (betOrder != null && betOrder.size() > 1) {
            return false;
        }

        return true;
    }
}
