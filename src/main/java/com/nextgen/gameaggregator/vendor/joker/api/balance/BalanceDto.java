package com.nextgen.gameaggregator.vendor.joker.api.balance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.joker.constant.ResponseCodes;
import lombok.Data;

import jakarta.validation.constraints.*;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BalanceDto {

    @NotBlank(message = ResponseCodes.INVALID_PARAMETERS)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.INVALID_SIGNATURE)
    @Size(min = 4, max = 32, message = ResponseCodes.INVALID_PARAMETERS)
    private String username;

    @NotNull(message = ResponseCodes.INVALID_PARAMETERS)
    @Digits(integer = 13, fraction = 0, message = ResponseCodes.INVALID_SIGNATURE)
    private Long timestamp;

    @NotBlank(message = ResponseCodes.INVALID_PARAMETERS)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.INVALID_SIGNATURE)
    private String appid;

    @NotBlank(message = ResponseCodes.INVALID_PARAMETERS)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.INVALID_SIGNATURE)
    private String hash;
}
