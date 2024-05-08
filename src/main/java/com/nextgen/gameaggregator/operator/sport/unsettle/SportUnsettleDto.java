package com.nextgen.gameaggregator.operator.sport.unsettle;

import lombok.Data;

@Data
public class SportUnsettleDto {
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
