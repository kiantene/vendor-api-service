package com.nextgen.gameaggregator.operator.wallet.balance;

import lombok.Data;

@Data
public class WalletBalanceDto {
    private String traceId;
    private String username;
    private String currency;
    private String token;
}
