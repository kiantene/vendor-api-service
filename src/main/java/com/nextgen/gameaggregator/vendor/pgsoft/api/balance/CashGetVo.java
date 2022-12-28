package com.nextgen.gameaggregator.vendor.pgsoft.api.balance;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CashGetVo {
    private String currencyCode;
    private BigDecimal balanceAmount;
    private Long updatedTime;
}
