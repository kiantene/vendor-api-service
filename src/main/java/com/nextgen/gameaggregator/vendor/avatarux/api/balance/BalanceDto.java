package com.nextgen.gameaggregator.vendor.avatarux.api.balance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceDto {

    @NotBlank
    private String authorization;

    @NotBlank
    private String xServerAuthorization;

    @NotBlank
    private String nativeId;

    private String playerId;

    @NotBlank
    private String game;

    @NotBlank
    private String provider;
}
