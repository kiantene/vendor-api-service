package com.nextgen.gameaggregator.operator.sport.unsettle;

import lombok.Data;

@Data
public class SportUnsettleDto {
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
