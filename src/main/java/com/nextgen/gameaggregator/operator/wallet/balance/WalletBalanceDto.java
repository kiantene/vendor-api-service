package com.nextgen.gameaggregator.operator.wallet.balance;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class WalletBalanceDto {

    private String playerUsername;
    private String traceId;
    private Long agentId;
    private String vendor;
    private String currency;
}
