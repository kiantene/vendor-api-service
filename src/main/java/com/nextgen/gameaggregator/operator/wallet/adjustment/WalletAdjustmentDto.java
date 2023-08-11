package com.nextgen.gameaggregator.operator.wallet.adjustment;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletAdjustmentDto {
    private String traceId;
    private String username;
    private String transactionId;
    private String externalTransactionId;
    private BigDecimal amount;
    private String currency;
    private String gameCode;
    private String roundId;
    private Long timestamp;
}
