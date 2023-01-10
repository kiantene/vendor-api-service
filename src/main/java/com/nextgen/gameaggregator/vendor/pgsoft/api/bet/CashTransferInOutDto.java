package com.nextgen.gameaggregator.vendor.pgsoft.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.operator.wallet.bet.BetData;
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
public class CashTransferInOutDto extends CommonDto implements BetData {

    /**
     * Authentication Information
     */
    @Size(min = 1, max = 100)
    //* Below are not mandatory
    private String operatorPlayerSession;

    /**
     * General Bet Information
     */

    //* Below are mandatory
    @NotEmpty
    @Size(min = 3, max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX) // Only alphanumeric allowed
    private String playerName;

    @NotNull
    @Range(min = 0)
    private Integer gameId;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX) // Only alphanumeric allowed
    private String parentBetId;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX) // Only alphanumeric allowed
    private String betId;

    @NotBlank
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
    public BigDecimal getAmount() {
        return this.betAmount;
    }

    @Override
    public String getRoundId() {
        return this.parentBetId;
    }

    @Override
    public String getGameId() {
        return String.valueOf(this.gameId);
    }

    @Override
    public Long getTimestamp() {
        return this.createTime;
    }
}
