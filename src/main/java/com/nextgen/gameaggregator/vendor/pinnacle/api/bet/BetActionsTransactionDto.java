package com.nextgen.gameaggregator.vendor.pinnacle.api.bet;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetActionsTransactionDto {
    private Long TransactionId;
    private String TransactionType;
    private String TransactionDate;
    private BigDecimal Amount;
}
