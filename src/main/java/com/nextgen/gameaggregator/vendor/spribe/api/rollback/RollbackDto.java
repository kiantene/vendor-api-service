package com.nextgen.gameaggregator.vendor.spribe.api.rollback;

import java.math.BigDecimal;

import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.util.ValidationUtils;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RollbackDto implements RollbackData {
    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String user_id;

    @NotNull
    private BigDecimal amount;

    @NotBlank
    private String provider;

    @NotBlank
    private String rollback_provider_tx_id;

    @NotBlank
    private String provider_tx_id;

    @NotBlank
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
        return provider_tx_id;
    }

    @Override
    public Long getVendorSettledTime() {
        return System.currentTimeMillis();
    }
}
