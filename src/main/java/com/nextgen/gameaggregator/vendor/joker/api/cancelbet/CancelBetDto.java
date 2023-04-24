package com.nextgen.gameaggregator.vendor.joker.api.cancelbet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.joker.constant.ResponseCodes;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CancelBetDto {

    @NotBlank(message = ResponseCodes.INVALID_APPID)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.INVALID_APPID)
    private String appid;

    @NotBlank(message = ResponseCodes.INVALID_SIGNATURE)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.INVALID_SIGNATURE)
    private String hash;

    @NotBlank(message = ResponseCodes.INVALID_PARAMETERS)
    @Pattern(regexp = "^[a-zA-Z0-9_:-]+$", message = ResponseCodes.INVALID_PARAMETERS)
    private String id;

    @NotBlank(message = ResponseCodes.INVALID_PARAMETERS)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.INVALID_PARAMETERS)
    private String betid;

    @NotBlank(message = ResponseCodes.INVALID_PARAMETERS)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.INVALID_PARAMETERS)
    @Size(min = 4, max = 32, message = ResponseCodes.INVALID_PARAMETERS)
    private String username;

    @NotNull(message = ResponseCodes.INVALID_PARAMETERS)
    @Digits(integer = 13, fraction = 0, message = ResponseCodes.INVALID_PARAMETERS)
    private Long timestamp;

    @NotBlank(message = ResponseCodes.INVALID_PARAMETERS)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.INVALID_PARAMETERS)
    private String gamecode;

    @NotBlank(message = ResponseCodes.INVALID_PARAMETERS)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.INVALID_PARAMETERS)
    private String roundid;

}
