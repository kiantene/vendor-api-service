package com.nextgen.gameaggregator.vendor.booongo.api.login;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class LoginArgsDto {

    @NotBlank
    private String platform;
}
