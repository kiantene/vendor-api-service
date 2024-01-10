package com.nextgen.gameaggregator.vendor.yeebet.api.balance;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
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
