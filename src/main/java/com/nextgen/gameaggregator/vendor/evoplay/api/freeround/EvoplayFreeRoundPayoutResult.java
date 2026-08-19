package com.nextgen.gameaggregator.vendor.evoplay.api.freeround;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class EvoplayFreeRoundPayoutResult {
    @Builder.Default
    BigDecimal balance = BigDecimal.ZERO;
    String currency;
}
