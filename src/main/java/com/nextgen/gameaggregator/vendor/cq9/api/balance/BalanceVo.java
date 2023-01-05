package com.nextgen.gameaggregator.vendor.cq9.api.balance;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BalanceVo {
    private BigDecimal balance;
    private String currency;
}
