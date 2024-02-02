package com.nextgen.gameaggregator.operator.sport.adjustment;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SportAdjustmentDto {
    private String traceId;
    private String username;
    private String transactionId;
    private String externalTransactionId;
    private String roundId;
    private BigDecimal amount;
    private String currency;
    private Long timestamp;
}
