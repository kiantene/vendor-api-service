package com.nextgen.gameaggregator.vendor.endorphina.api.balance;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class BalanceResponse {

    private BigDecimal balance;

}