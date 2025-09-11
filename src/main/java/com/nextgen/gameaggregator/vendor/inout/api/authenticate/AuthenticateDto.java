package com.nextgen.gameaggregator.vendor.inout.api.authenticate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class AuthenticateDto {
    @NotBlank
    @Size(max = 255)
    private String currency;

    @NotBlank
    @Size(max = 255)
    private String operator;

    @NotBlank
    @Size(max = 255)
    private String gameMode;
}
