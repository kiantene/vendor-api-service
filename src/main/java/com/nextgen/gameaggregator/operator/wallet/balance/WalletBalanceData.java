package com.nextgen.gameaggregator.operator.wallet.balance;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletBalanceData {
    private String username;
    private String currency;
    private BigDecimal balance;
}
