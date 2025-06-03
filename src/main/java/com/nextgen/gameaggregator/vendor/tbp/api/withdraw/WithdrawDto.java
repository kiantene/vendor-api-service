package com.nextgen.gameaggregator.vendor.tbp.api.withdraw;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.*;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class WithdrawDto implements BetResultData {

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("UserName")
    private String username;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("Password")
    private String password;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("PlayerId")
    private String playerId;

    @NotBlank
    @Size(max = 255)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("RoundIdBI")
    private String roundIdBI;

    @NotBlank
    @Size(max = 255)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("TransferId")
    private String transferId;

    @JsonProperty("CasinoTransferId")
    private String casinoTransferId;

    @NotNull
    @JsonProperty("GameId")
    private Long gameIdv;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("GameNumber")
    private String gameNumber;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    @JsonProperty("SessionId")
    private String sessionId;

    @NotNull
    @Digits(integer = 20, fraction = 8)
    @JsonProperty("Amount")
    private BigDecimal amount;

    @NotBlank
    @JsonProperty("Currency")
    private String currency;

    @NotBlank
    @JsonProperty("Reason")
    private String reason;

    @NotBlank
    @JsonProperty("PlatformType")
    private String platformType;

    @JsonProperty("IsBonusSpin")
    private Boolean isBonusSpin;

    @JsonProperty("CampaignIdFromOperator")
    private String campaignIdFromOperator;

    @JsonProperty("BonusSpinsRemaining")
    private Integer bonusSpinsRemaining;

    @Override
    public String getExternalTransactionId() {
        return this.transferId;
    }

    @Override
    public String getVendorBetId() {
        return this.transferId;
    }

    @Override
    public String getRoundId() {
        return this.roundIdBI;
    }

    @Override
    public String getGameId() {
        return null;
    }

    @Override
    public BigDecimal getBetAmount() {
        return this.amount;
    }

    @Override
    public BigDecimal getWinAmount() {
        return null;
    }

    @Override
    public BigDecimal getWinLoss() {
        return null;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return null;
    }

    @Override
    public Long getVendorBetTime() {
        return System.currentTimeMillis();
    }

    @Override
    public Long getResultTime() {
        return null;
    }

    @Override
    public Long getVendorSettleTime() {
        return null;
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public Integer getIsFreespin() {
        if (Boolean.TRUE.equals(this.isBonusSpin)) {
            return 1;
        }
        return 0;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.UNSETTLED;
    }

    @Override
    public boolean getShouldSettleByBet() {
        return BetResultData.super.getShouldSettleByBet();
    }
}