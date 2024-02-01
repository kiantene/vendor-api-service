package com.nextgen.gameaggregator.vendor.alize.api.betNSettle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetNSettleDto implements BetResultData {
    @NotBlank
    @Size(max = 36)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String betId;

    @NotBlank
    @Size(max = 36)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String roundId;

    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String token;

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String gameCode;

    // Optional field
    private Integer gameId;

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String username;

    @NotBlank
    @Size(max = 5)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String currency;

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
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
    private Long betTime;

    @NotNull
    private Long processedTime;

    @NotNull
    private Long timestamp;

    private String info;

    private String ip;

    @Override
    public String getExternalTransactionId() {
        return this.betId;
    }

    @Override
    public String getVendorBetId() {
        return this.betId;
    }

    @Override
    public String getRoundId() {
        return this.roundId;
    }

    @Override
    public String getGameId() {
        return this.gameCode;
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.stake;
    }

    @Override
    public BigDecimal getWinAmount() {
        return this.payout;
    }

    @Override
    public BigDecimal getWinLoss() {
        return this.winloss;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return this.stake;
    }

    @Override
    public Long getVendorBetTime() {
        return this.betTime;
    }

    @Override
    public Long getResultTime() {
        return System.currentTimeMillis();
    }

    @Override
    public Long getVendorSettleTime() {
        return this.processedTime;
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
