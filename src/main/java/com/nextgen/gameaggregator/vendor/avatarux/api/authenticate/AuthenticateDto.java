package com.nextgen.gameaggregator.vendor.avatarux.api.authenticate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthenticateDto {
    @NotBlank
    private String xServerAuthorization;

    @NotBlank
    private String operator;

    @NotBlank
    private String key;
}
