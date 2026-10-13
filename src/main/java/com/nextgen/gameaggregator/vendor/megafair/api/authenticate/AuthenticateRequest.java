package com.nextgen.gameaggregator.vendor.megafair.api.authenticate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthenticateRequest {

    @NotBlank
    @Size(max = 255)
    private String token;

    @NotBlank
    @Size(max = 255)
    private String gameSessionId;

    @NotBlank
    @Size(max = 255)
    private String hash;

}