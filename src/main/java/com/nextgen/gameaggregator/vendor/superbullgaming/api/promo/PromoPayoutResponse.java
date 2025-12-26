package com.nextgen.gameaggregator.vendor.superbullgaming.api.promo;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Builder
@Getter
public class PromoPayoutResponse {
    private BigDecimal balance;
    private String currency;
    private String username;
}
