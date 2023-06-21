package com.nextgen.gameaggregator.vendor.alizegames.api.authenticate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthenticateDto {
    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX) // Only alphanumeric/underscore/dash allowed
    private String token;

    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX) // Only alphanumeric/underscore/dash allowed
    private String gameCode;

    @NotBlank
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX) // Only alphanumeric allowed
    private String operatorId;

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX) // Only alphanumeric allowed
    private String username;

    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_REGEX) // Only alphanumeric allowed
    private String currency;

    @NotNull
    private Long timestamp;
}
