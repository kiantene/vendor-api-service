package com.nextgen.gameaggregator.vendor.spribe.api.result;

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
    private String session_token;

    @NotNull
    @PositiveOrZero
    private BigDecimal amount;

    @NotBlank
    private String game;

    @NotBlank
    private String user_id;

    @NotNull
    private String action_id;

    @NotBlank
    private String action;

    @NotBlank
    private String provider;

    @NotBlank
    private String provider_tx_id;

    @NotBlank
    @Size(max = 3)
    private String currency;

    @NotBlank
    private String platform;

    private String withdraw_provider_tx_id;

    @Override
    public String getExternalTransactionId() {
        return provider_tx_id;
    }

    @Override
    public String getVendorBetId() {
        return provider_tx_id;
    }

    @Override
    public String getRoundId() {
        return action_id;
    }

    @Override
    public String getGameId() {
        return game;
    }

    @Override
    public BigDecimal getBetAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getWinAmount() {
        return amount;
    }

    @Override
    public BigDecimal getWinLoss() {
        return null;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return BigDecimal.ZERO;
    }

    @Override
    public Long getVendorBetTime() {
        return System.currentTimeMillis();
    }

    @Override
    public Long getResultTime() {
        return System.currentTimeMillis();
    }

    @Override
    public Long getVendorSettleTime() {
        return System.currentTimeMillis();
    }

    @Override
    public BigDecimal getJackpotAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public Integer getIsFreespin() {
        return (getAction().equals("freebet")) ? 1 : 0;
    }

    @Override
    public BetStatus getBetStatus() {
        return BetStatus.SETTLED;
    }
    
}
