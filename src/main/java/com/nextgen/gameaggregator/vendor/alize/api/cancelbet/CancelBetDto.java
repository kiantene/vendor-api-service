package com.nextgen.gameaggregator.vendor.alize.api.cancelbet;

import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.util.ValidationUtils;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CancelBetDto implements RollbackData {
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
    private Long timestamp;

    private String ip;

    @Override
    public String getRollbackId() {
        return betId;
    }

    @Override
    public Long getVendorSettledTime() {
        return timestamp;
    }
}
