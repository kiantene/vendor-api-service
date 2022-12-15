package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_181.authenticate;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class AuthenticateDto {
    @NotBlank
    private String hash;

    @NotBlank
    private String token;

    @NotBlank
    private String providerId;
}
