package com.nextgen.gameaggregator.vendor.groove.api.balance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BalanceRequest {

    @NotBlank
    @Size(max = 60)
    private String accountid;

    @NotBlank
    @Size(max = 255)
    private String apiversion;

    @NotBlank
    @Size(max = 255)
    private String device;

    @NotBlank
    @Size(max = 64)
    private String gamesessionid;

    @NotBlank
    @Size(max = 255)
    private String nogsgameid;

    @NotBlank
    @Size(max = 255)
    private String request;
}
