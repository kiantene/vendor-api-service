package com.nextgen.gameaggregator.util;

import lombok.Data;
@Data
public class TestWalletBetDto {

    private String traceId;
    private String username;
    private String transactionId;
    private String externalTransactionId;
    private String amount;
    private String currency;
    private String token;
    private String gameCode;
    private String roundId;
    private Long timestamp;
}
