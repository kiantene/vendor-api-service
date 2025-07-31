package com.nextgen.gameaggregator.vendor.aviatorstudio.api.cashin;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CashInResponse {
    private String id;
    private BigDecimal balance;
    private String username;
    private Integer error;
    private String message;
}
