package com.nextgen.gameaggregator.vendor.pgsoft.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.enums.WinType;
import com.nextgen.gameaggregator.operator.wallet.bet.BetData;
import com.nextgen.gameaggregator.operator.wallet.settled.UnsettledResultSettledData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.pgsoft.dto.CommonDto;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import org.hibernate.validator.constraints.Range;
import org.intellij.lang.annotations.RegExp;

import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CashTransferInOutDto implements UnsettledResultSettledData {

    @Size(min = 1, max = 100)
    @NotBlank
    //* Below are not mandatory
    private String operatorPlayerSession;

    @Size(min = 1, max = 100)
    @NotBlank
    private String operatorToken;

    @Size(min = 1, max = 100)
    @NotBlank
    private String secretKey;

    //* Below are mandatory
    @NotEmpty
    @Size(min = 3, max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX) // Only alphanumeric allowed
    private String playerName;

    @NotNull
    private String gameId;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX) // Only alphanumeric allowed
    private String parentBetId;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX) // Only alphanumeric allowed
    private String betId;

    @NotBlank
    @Size(min = 1, max = 45)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX) // Only alphanumeric allowed
    private String currencyCode;

    @NotNull
    @Range(min = 0)
    @Digits(integer = 8, fraction = 2)
    private BigDecimal betAmount;

    @NotNull
    @Range(min = 0)
    @Digits(integer = 8, fraction = 2)
    private BigDecimal winAmount;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    private BigDecimal transferAmount;

    @NotBlank
    private String transactionId;

    @NotNull
    @Range(min = 1, max = 3)
    private Integer betType;

    @Positive
    @NotNull
    private Long createTime;

    @Positive
    @NotNull
    private Long updatedTime;

    //* Below are not mandatory
    private String walletType;
    private String platform;

    /**
     * Bet Indicator
     */
    //* Below are not mandatory
    private Boolean isValidateBet;
    private Boolean isAdjustment;
    private Boolean isParentZeroStake;
    private Boolean isFeature;
    private Boolean isFeatureBuy;
    private Boolean isWager;
    private Boolean isEndRound;

    /**
     * Free Game Information
     */
    //* Below are not mandatory
    private String freeGameTransactionId;
    private String freeGameName;
    private Integer freeGameId;
    private Boolean isMinusCount;

    /**
     * Bonus Game Information
     */
    //* Below are not mandatory
    private String bonusTransactionId;
    private String bonusName;
    private Integer bonusId;
    private BigDecimal bonusBalanceAmount;
    private BigDecimal bonusRatioAmount;

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
        return this.parentBetId;
    }

    @Override
    public BigDecimal getWinLoss() {
        return (this.winAmount.subtract(this.betAmount));
    }

    @Override
    public BigDecimal getVendorWinLoss() {
        return this.getWinLoss();
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return this.betAmount;
    }

    @Override
    public BigDecimal getRefundAmount() {
        return BigDecimal.valueOf(0);
    }

    @Override
    public WinType getResultType() {
        return (this.getWinLoss().compareTo(BigDecimal.ZERO) >= 0)?WinType.WIN:WinType.LOSE;
    }

    @Override
    public Long getVendorBetTime() {
        return this.createTime;
    }

    @Override
    public Long getResultTime() {
        return this.updatedTime;
    }

    @Override
    public Long getVendorSettleTime() {
        return this.updatedTime;
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public Integer getIsCancelled() {
        return 0;
    }

    @Override
    public Integer getIsFreespin() {
        return 0;
    }
}
