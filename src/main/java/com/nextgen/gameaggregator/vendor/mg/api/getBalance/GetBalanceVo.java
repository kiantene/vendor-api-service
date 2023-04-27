package com.nextgen.gameaggregator.vendor.mg.api.getBalance;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class GetBalanceVo {
    private String currency;
    private BigDecimal balance;
}
