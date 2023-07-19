package com.nextgen.gameaggregator.vendor.alize.api.result;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SettleDto implements BetResultData {
    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX) // Only alphanumeric/underscore/dash allowed
    private String betId;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX) // Only alphanumeric/underscore/dash allowed
    private String roundId;

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX) // Only alphanumeric/underscore/dash allowed
    private String token;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX) // Only alphanumeric/underscore/dash allowed
    private String gameCode;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX) // Only alphanumeric allowed
    private String username;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX) // Only alphanumeric allowed
    private String currency;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX) // Only alphanumeric allowed
    private String operatorId;

    @NotNull
    @PositiveOrZero
    private BigDecimal stake;

    @NotNull
    @PositiveOrZero
    private BigDecimal payout;

    @NotNull
    private BigDecimal winloss;

    @NotNull
    private Long timestamp;

    private String info;

    private String ip;

    @Override
    public String getExternalTransactionId() {
        return betId;
    }

    @Override
    public String getVendorBetId() {
        return betId;
    }

    @Override
    public String getRoundId() {
        return roundId;
    }

    @Override
    public String getGameId() {
        return gameCode;
    }

    @Override
    public BigDecimal getBetAmount() {
        return stake;
    }

    @Override
    public BigDecimal getWinAmount() {
        return payout;
    }

    @Override
    public BigDecimal getWinLoss() {
        return winloss;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return stake;
    }

    @Override
    public Long getVendorBetTime() {
        return null;
    }

    @Override
    public Long getResultTime() {
        return timestamp;
    }

    @Override
    public Long getVendorSettleTime() {
        return timestamp;
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public Integer getIsFreespin() {
        return (getStake().equals(BigDecimal.ZERO) ? 1 : 0);
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.SETTLED;
    }
}
