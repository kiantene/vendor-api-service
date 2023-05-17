package com.nextgen.gameaggregator.vendor.alizegames.api.authenticate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthenticateDto {
    // Token of the player
    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX) // Only alphanumeric/underscore/dash allowed
    private String token;

    // Username
    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX) // Only alphanumeric/underscore/dash allowed
    private String username;

    // Currency
    @NotBlank
    private String currency;

    // OperatorId
    @NotBlank
    private String operatorId;

    @NotBlank
    private Long timestamp;
}
