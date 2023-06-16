package com.nextgen.gameaggregator.vendor.booongo.api.login;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.booongo.constant.ResponseCodes;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginArgsDto {

    @NotBlank(message = ResponseCodes.GAME_NOT_ALLOWED)
    private String platform;
}
