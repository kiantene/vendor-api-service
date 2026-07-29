package com.nextgen.gameaggregator.vendor.wazdan.api.authenticate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthenticateRequest {

    @NotBlank
    @Size(max = 255)
    private String token;

    @Size(max = 255)
    private String ip;

    @NotNull
    private int gameId;
}
