package com.nextgen.gameaggregator.vendor.yeebet.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceDto {

    @NotBlank
    private String appid;

    @NotBlank
    private String username;

    @NotBlank
    private String notifyid;

    @NotBlank
    private String sign;
}
