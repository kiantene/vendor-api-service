package com.nextgen.gameaggregator.vendor.mg.api.login;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginDto {
    @NotBlank
    @Size(min = 1, max = 50)
    private String playerId;
}
