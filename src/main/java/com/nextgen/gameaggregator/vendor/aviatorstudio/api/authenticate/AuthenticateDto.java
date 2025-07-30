package com.nextgen.gameaggregator.vendor.aviatorstudio.api.authenticate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthenticateDto {
    private String authorization;

    @NotBlank
    @Size(max = 5)
    private String currency;

    @NotBlank
    @Size(max = 255)
    private String sessionId;
}
