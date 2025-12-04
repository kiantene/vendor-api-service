package com.nextgen.gameaggregator.vendor.ezugi.api.v2.authenticate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthenticateRequest {
    @NotBlank
    @Size(min = 1, max = 250)
    private String token;
    
    @NotNull
    @JsonProperty("operatorId")
    private Integer operatorId;
    
    @NotNull
    @JsonProperty("platformId")
    private Integer platformId;

    @NotNull
    @Digits(integer = 13, fraction = 0)
    @JsonProperty("timestamp")
    private Long timestamp;
}