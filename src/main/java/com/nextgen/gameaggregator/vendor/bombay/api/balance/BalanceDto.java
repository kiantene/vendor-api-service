package com.nextgen.gameaggregator.vendor.bombay.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bombay.constant.ResponseCodes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceDto {
    @NotBlank(message = ResponseCodes.RS_ERROR_INVALID_TOKEN)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.RS_ERROR_INVALID_TOKEN)
    private String token;

    @NotBlank(message = ResponseCodes.RS_ERROR_WRONG_CURRENCY)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.RS_ERROR_WRONG_CURRENCY)
    private String currency;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String request_uuid;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String transaction_uuid;

    @NotBlank(message = ResponseCodes.RS_ERROR_INVALID_GAME)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.RS_ERROR_INVALID_GAME)
    private String game_id;
}
