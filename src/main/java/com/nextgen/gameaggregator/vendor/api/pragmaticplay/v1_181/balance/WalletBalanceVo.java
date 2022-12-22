package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_181.balance;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletBalanceVo {

    private String currency;
    private BigDecimal cash;
    private BigDecimal bonus;
    private Integer error;
    private String description;

}
