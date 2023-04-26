package com.nextgen.gameaggregator.operator.wallet.rollback;

import lombok.Data;

@Data
public class WalletRollbackDto {
    private String traceId;
    private String transactionId;
    private String betId;
    private String externalTransactionId;
    private String roundId;
    private String gameCode;
    private String username;
    private String currency;
    private Long timestamp;
}
