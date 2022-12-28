package com.nextgen.gameaggregator.vendor.pragmaticplay.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceDto {
    @NotBlank
    private String hash;
    @NotBlank
    private String providerId;
    @NotBlank
    private String userId;
    @NotBlank
    private String token;
}
