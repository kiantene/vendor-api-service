package com.nextgen.gameaggregator.vendor.aviatorstudio.api.bet;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class BetResponse {
    private String id;
    private BigDecimal balance;
    private String username;
}
