package com.nextgen.gameaggregator.operator.wallet.refund;

import lombok.Data;

@Data
public class WalletRefundDto {
    private String traceId;
    private String username;
    private String transactionId;
    private String externalTransactionId;
    private String referenceTransactionId;
    private String gameCode;
    private String roundId;
    private Long timestamp;
}
