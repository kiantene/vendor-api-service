package com.nextgen.gameaggregator.vendor.spribe.api.v2.authenticate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AuthRequest {
    @NotBlank
    private String userToken;

    @NotBlank
    private String sessionToken;

    @NotBlank
    private String platform;

    @NotBlank
    private String currency;
}
