package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_181.authenticate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthenticateDto {
    @NotBlank
    private String hash;
    @NotBlank
    private String token;
    @NotBlank
    private String providerId;
    @NotBlank
    private String gameId;
}
