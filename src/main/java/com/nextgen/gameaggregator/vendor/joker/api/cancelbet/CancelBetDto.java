package com.nextgen.gameaggregator.vendor.joker.api.cancelbet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.joker.constant.ResponseCodes;
import lombok.Data;

import javax.validation.constraints.*;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CancelBetDto {
    @NotBlank(message = ResponseCodes.OTHER_MESSAGE)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.OTHER_MESSAGE)
    private String appid;

    @NotBlank(message = ResponseCodes.OTHER_MESSAGE)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.OTHER_MESSAGE)
    private String betid;

    @NotBlank(message = ResponseCodes.OTHER_MESSAGE)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.OTHER_MESSAGE)
    private String hash;

    @NotBlank(message = ResponseCodes.OTHER_MESSAGE)
    private String id;

    @NotBlank(message = ResponseCodes.OTHER_MESSAGE)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.OTHER_MESSAGE)
    @Size(min = 4, max = 32, message = ResponseCodes.OTHER_MESSAGE)
    private String username;

    @NotNull(message = ResponseCodes.OTHER_MESSAGE)
    @Digits(integer = 13, fraction = 0, message = ResponseCodes.OTHER_MESSAGE)
    private Long timestamp;

    @NotBlank(message = ResponseCodes.OTHER_MESSAGE)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.OTHER_MESSAGE)
    private String gamecode;

    @NotBlank(message = ResponseCodes.OTHER_MESSAGE)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX, message = ResponseCodes.OTHER_MESSAGE)
    private String roundid;
}
