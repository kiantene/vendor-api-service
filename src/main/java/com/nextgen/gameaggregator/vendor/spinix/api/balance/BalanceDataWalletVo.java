package com.nextgen.gameaggregator.vendor.spinix.api.balance;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BalanceDataWalletVo {

    private String currency;
    private BigDecimal balance;

}