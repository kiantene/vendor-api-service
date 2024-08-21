package com.nextgen.gameaggregator.vendor.spribe.api.rollback;

import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.util.ValidationUtils;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RollbackDto implements RollbackData {
    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String user_id;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    private String provider;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String rollback_provider_tx_id;

    @NotBlank
    private String provider_tx_id;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String game;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String session_token;

    @NotBlank
    private String action;

    @NotBlank
    private String action_id;

    @Override
    public String getRollbackId() {
        return rollback_provider_tx_id;
    }

    @Override
    public Long getVendorSettledTime() {
        return System.currentTimeMillis();
    }

    @Override
    public String getRoundId() {
        return null;
    }
}
