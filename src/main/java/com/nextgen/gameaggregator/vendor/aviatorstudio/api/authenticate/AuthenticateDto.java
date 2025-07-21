package com.nextgen.gameaggregator.vendor.aviatorstudio.api.authenticate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthenticateDto {

    @NotBlank
    private String authorization;

    @NotBlank
    private String currency;

    @NotBlank
    private String sessionId;

}
