package com.nextgen.gameaggregator.vendor.endorphina.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceRequest {

    @NotBlank
    @Size(max = 255)
    private String token;

    @NotBlank
    @Size(max = 255)
    private String sign;
}