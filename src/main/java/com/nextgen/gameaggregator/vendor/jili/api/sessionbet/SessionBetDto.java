package com.nextgen.gameaggregator.vendor.jili.api.sessionbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.jili.constant.Formats;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.math.BigInteger;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionBetDto implements BetResultData {
    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 50)
    private String reqId;
    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @Size(min = 1, max = 50)
    private String token;
    @NotBlank
    private String currency;
    @PositiveOrZero
    @NotNull
    private Integer game;
    @Positive
    @NotNull
    private BigInteger round;
    @Pattern(regexp = "^true$|^false$")
    private String offline;
    @Positive
    @NotNull
    @Range(min = 0, max = 2147483647)
    private BigInteger wagersTime;
    @NotNull
    @Range(min = 0)
    @Digits(integer = 12, fraction = 4)
    private BigDecimal betAmount;
    @NotNull
    @Digits(integer = 12, fraction = 4)
    private BigDecimal winloseAmount;
    @Positive
    @NotNull
    private BigInteger sessionId;
    @Positive
    @NotNull
    private Integer type;
    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String userId;
    @NotNull
    @Digits(integer = 12, fraction = 4)
    private BigDecimal turnover;
    @NotNull
    @Digits(integer = 12, fraction = 4)
    private BigDecimal preserve;
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX)
    private String platform;
    @Digits(integer = 12, fraction = 4)
    private BigDecimal sessionTotalBet;
    @Positive
    private Integer statementType;

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
        return this.getBetAmount().negate();
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        if (this.type == 1) {
            return this.turnover;
        }
        return null;

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
    public BigDecimal getJackpotAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public Integer getIsFreespin() {
        return 0;
    }

    /**
     * @return
     */
    @Override
    public BetStatus getBetStatus() {
        if (this.type == Formats.SESSION_BET_TYPE_SETTLE) {
            return BetStatus.SETTLED;
        }

        return BetStatus.UNSETTLED;
    }

    private Long getTimestamp() {
        long timestamp = this.getWagersTime().longValueExact();
        if (String.valueOf(Math.abs(timestamp)).length() > 10) {
            return timestamp;
        }
        return timestamp * 1000;
    }
}
