package com.nextgen.gameaggregator.vendor.aviatorstudio.api.cashout;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CashOutResponse {
    private String id;
    private BigDecimal balance;
    private String username;
    private Integer error;
    private String message;
}
