package com.nextgen.gameaggregator.operator.wallet.win;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletWinDto {
    private String traceId;
    private String username;
    private String transactionId;
    private String externalTransactionId;
    private String referenceTransactionId;
    private BigDecimal amount;
    private String currency;
    private String token;
    private String gameId;
    private String roundId;
    private String winType;
    private Long timestamp;
}
