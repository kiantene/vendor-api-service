package com.nextgen.gameaggregator.vendor.smartsoft.api.authenticate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthenticateDto {
    @NotBlank
    private String signature;

    @NotBlank
    @JsonProperty("Token")
    private String token;
}
