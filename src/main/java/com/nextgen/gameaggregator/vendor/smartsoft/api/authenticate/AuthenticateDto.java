package com.nextgen.gameaggregator.vendor.smartsoft.api.authenticate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthenticateDto {
    @NotBlank
    private String token;
}
