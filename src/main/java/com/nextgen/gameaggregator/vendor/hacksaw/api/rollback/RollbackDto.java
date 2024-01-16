package com.nextgen.gameaggregator.vendor.hacksaw.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.hacksaw.api.action.ActionDto;
import com.nextgen.gameaggregator.vendor.hacksaw.api.bet.FreeRoundDto;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollbackDto extends ActionDto implements RollbackData {

    @NotBlank
    @Size(min = 1, max = 64)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String externalPlayerId;

    @NotNull
    @PositiveOrZero
    private Long amount;

    @NotBlank
    @Size(min = 1, max = 4)
    private String currency;

    @NotNull
    private Long gameSessionId;

    @NotBlank
    @Size(min = 1, max = 64)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String externalSessionId;

    @NotNull
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private Long transactionId;

    @NotNull
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private Long rolledBackTransactionId;

    // variable to check it is free spin or not
    private FreeRoundDto freeRoundData;

    @Override
    public String getRollbackId() {
        return String.valueOf(this.getTransactionId());
    }

    @Override
    public Long getVendorSettledTime() {
        return null;
    }

}
