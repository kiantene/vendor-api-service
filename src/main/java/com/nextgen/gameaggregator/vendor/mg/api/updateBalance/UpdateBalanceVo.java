package com.nextgen.gameaggregator.vendor.mg.api.updateBalance;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class UpdateBalanceVo {
    private String extTxnId;
    private String currency;
    private BigDecimal balance;
    private Long extCreationTimeMs;
}