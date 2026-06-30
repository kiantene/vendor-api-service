package com.nextgen.gameaggregator.vendor.groove.api.authenticate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthenticateRequest {

    @NotBlank
    @Size(max = 60)
    private String accountid;

    @NotBlank
    @Size(max = 255)
    private String apiversion;

    @NotBlank
    @Size(max = 255)
    private String device;

    @NotBlank
    @Size(max = 64)
    private String gamesessionid;

    @NotBlank
    @Size(max = 255)
    private String request;
}
