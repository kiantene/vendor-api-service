package com.nextgen.gameaggregator.operator.sport.refund;

import lombok.Data;

@Data
public class SportRefundDto {
    private String traceId;
    private String username;
    private String transactionId;
    private String externalTransactionId;
    private String betId;
    private String roundId;
    private String gameCode;
    private String currency;
    private Long timestamp;
}
