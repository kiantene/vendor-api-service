package com.nextgen.gameaggregator.vendor.koolbet.api.v2.authenticate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.util.ValidationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthenticateRequest {
    @NotBlank
    @Size(max = 255)
    @Pattern(regexp = ValidationUtils.ALPHANUMERIC_DASH_REGEX)
    private String reqId;

    @NotBlank
    @Size(max = 255)
    private String token;
}
