package com.nextgen.gameaggregator.vendor.spribe.api.bet;

import java.math.BigDecimal;

import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.util.ValidationUtils;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class BetDto implements BetResultData {
    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String user_id;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String session_token;

    @NotBlank
    @Size(max = 3)
    private String currency;

    @NotNull
    @PositiveOrZero
    private BigDecimal amount;

    @NotBlank
    private String game;

    @NotBlank
    private String action_id;

    @NotBlank
    private String action;

    @NotBlank
    private String provider;

    @NotBlank
    private String provider_tx_id;

    @NotBlank
    private String platform;

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
        return amount;
    }

    @Override
    public BigDecimal getWinAmount() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getWinLoss() {
        return null;
    }

    @Override
    public BigDecimal getEffectiveTurnover() {
        return amount;
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
