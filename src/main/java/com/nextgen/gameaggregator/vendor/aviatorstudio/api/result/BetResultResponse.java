package com.nextgen.gameaggregator.vendor.aviatorstudio.api.result;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class BetResultResponse {
    private String id;
    private BigDecimal balance;
    private String username;
}
