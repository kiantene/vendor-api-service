package com.nextgen.gameaggregator.vendor.aviatorstudio.api.authenticate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthenticateRequest {
    @NotBlank
    @Size(max = 5)
    private String currency;

    @NotBlank
    @Size(max = 255)
    private String sessionId;
}
