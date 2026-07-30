package com.nextgen.gameaggregator.vendor.groove.api.betdetail;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GrooveLoginResponseVo {

    @NotBlank(message = "Jwt Token cannot be null")
    private String jwtToken;

}
