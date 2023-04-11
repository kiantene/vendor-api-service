package com.nextgen.gameaggregator.operator.wallet.bet;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletBetDto {
    private String traceId;
    private String username;
    private String transactionId;
    private String externalTransactionId;
    private BigDecimal amount;
    private String currency;
    private String token;
    private String gameCode;
    private String roundId;
    private Long timestamp;
}
