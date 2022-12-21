package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_181.balance;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class WalletBalanceDto {

    @NotBlank
    private String token;
    @NotBlank
    private String hash;
    @NotBlank
    private String providerId;
    @NotBlank
    private String userId;
}
