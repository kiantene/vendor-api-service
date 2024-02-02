package com.nextgen.gameaggregator.vendor.pinnacle.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionsTransactionDto {
    @JsonProperty("TransactionId")
    private Long transactionId;

    @JsonProperty("TransactionType")
    private String transactionType;

    @JsonProperty("TransactionDate")
    private String transactionDate;

    @JsonProperty("Amount")
    private BigDecimal amount;
}
