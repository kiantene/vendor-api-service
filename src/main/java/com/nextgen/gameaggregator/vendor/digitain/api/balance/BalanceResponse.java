package com.nextgen.gameaggregator.vendor.digitain.api.balance;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class BalanceResponse {

    private Integer err;

    private BigDecimal bln;

}