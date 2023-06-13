package com.nextgen.gameaggregator.vendor.booongo.api.login;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginArgsDto {

    @NotBlank
    private String platform;
}
