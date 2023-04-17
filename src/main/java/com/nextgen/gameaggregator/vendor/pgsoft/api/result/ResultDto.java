package com.nextgen.gameaggregator.vendor.pgsoft.api.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nextgen.gameaggregator.entity.BetHistory;
import com.nextgen.gameaggregator.entity.BetResultLog;
import com.nextgen.gameaggregator.enums.WinType;
import com.nextgen.gameaggregator.operator.wallet.win.WinData;
import com.nextgen.gameaggregator.vendor.pgsoft.dto.CommonDto;
import lombok.Data;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ResultDto extends CommonDto implements WinData {

    /**
     * Authentication Information
     */
    //* Below are not mandatory
    private String operatorPlayerSession;

    /**
     * General Bet Information
     */

    //* Below are mandatory
    @NotBlank
    private String playerName;

    @NotNull
    @Positive
    private Integer gameId;

    @NotBlank
    private String parentBetId;

    @NotBlank
    private String betId;

    @NotBlank
    private String currencyCode;

    @NotNull
    private BigDecimal betAmount;

    @NotNull
    private BigDecimal winAmount;

    @NotNull
    private BigDecimal transferAmount;

    @NotBlank
    private String transactionId;

    @NotNull
    @Positive
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
        return this.winAmount;
    }

    @Override
    public String getRoundId() {
        return this.parentBetId;
    }

    @Override
    public Long getTimestamp() {
        return this.createTime;
    }

    @Override
    public String getGameId() {
        return String.valueOf(this.gameId);
    }

    @Override
    public WinType getWinType() {
        return (this.getWinAmount().compareTo(BigDecimal.ZERO) > 0) ? WinType.WIN : WinType.LOSE;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return BigDecimal.valueOf(0);
    }

    @Override
    public BetResultLog prepareData(BetHistory betHistory, BetResultLog betResultLog) {
        return betResultLog;
    }
}