package com.nextgen.gameaggregator.vendor.mg.api.login;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginDto {
    @NotBlank
    @Size(min = 1, max = 50)
    private String playerId;
}
