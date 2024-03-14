package com.nextgen.gameaggregator.vendor.bombay.api.rollback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.wallet.rollback.RollbackData;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bombay.constant.ResponseCodes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RollbackDto implements RollbackData {
    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String transaction_uuid;

    @NotBlank(message = ResponseCodes.RS_ERROR_INVALID_TOKEN)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.RS_ERROR_INVALID_TOKEN)
    private String token;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String request_uuid;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String reference_transaction_uuid;

    @NotNull
    @PositiveOrZero
    private Integer amount;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String currency;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String round;

    @NotNull(message = ResponseCodes.RS_ERROR_INVALID_GAME)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.RS_ERROR_INVALID_GAME)
    private String game_id;

    @Override
    public String getRollbackId() {
        return this.reference_transaction_uuid;
    }

    @Override
    public Long getVendorSettledTime() {
        return System.currentTimeMillis(); //unix timestamp with millisecond;
    }
}
