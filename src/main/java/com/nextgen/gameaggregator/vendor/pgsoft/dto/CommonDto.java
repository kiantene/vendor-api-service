package com.nextgen.gameaggregator.vendor.pgsoft.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class CommonDto {
    @NotBlank
    @JsonProperty("operator_token")
    private String operatorToken;
    @NotBlank
    @JsonProperty("secret_key")
    private String secretKey;
}
