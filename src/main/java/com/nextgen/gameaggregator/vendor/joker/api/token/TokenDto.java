package com.nextgen.gameaggregator.vendor.joker.api.token;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.joker.constant.ResponseCodes;
import lombok.Data;

import jakarta.validation.constraints.*;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenDto {

    @NotBlank(message = ResponseCodes.INVALID_TOKEN)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.INVALID_TOKEN)
    @Size(min = 1, max = 64, message = ResponseCodes.INVALID_TOKEN)
    private String token;

    //TODO check valid IP
    @NotNull(message = ResponseCodes.INVALID_PARAMETERS)
    @Pattern(regexp = "^[a-zA-Z0-9_.:-]+$", message = ResponseCodes.INVALID_PARAMETERS)
    private String ip;

    @NotNull(message = ResponseCodes.INVALID_PARAMETERS)
    @Digits(integer = 13, fraction = 0, message = ResponseCodes.INVALID_PARAMETERS)
    private Long timestamp;

    @NotBlank(message = ResponseCodes.INVALID_PARAMETERS)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.INVALID_PARAMETERS)
    private String appid;

    @NotBlank(message = ResponseCodes.INVALID_SIGNATURE)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.INVALID_SIGNATURE)
    private String hash;
}
