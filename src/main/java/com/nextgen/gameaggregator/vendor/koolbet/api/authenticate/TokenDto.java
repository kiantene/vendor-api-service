package com.nextgen.gameaggregator.vendor.koolbet.api.authenticate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenDto {

    @NotBlank
    @JsonProperty("reqId")
    public String reqId;

    @NotBlank
    public String token;
}
