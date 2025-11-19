package com.nextgen.gameaggregator.vendor.mg.api.promo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PromoPayoutResponse {
    private String extTxnId;
    private String currency;
    private BigDecimal balance;
    private long extCreationTimeMs;
}
