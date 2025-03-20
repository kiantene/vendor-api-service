package com.nextgen.gameaggregator.vendor.cg.api.authentication;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthenticationDto {
    @NotBlank
    @Size(max = 255)
    String token;
}
