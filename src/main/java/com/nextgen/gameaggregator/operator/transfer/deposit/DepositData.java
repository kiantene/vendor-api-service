package com.nextgen.gameaggregator.operator.transfer.deposit;

import lombok.Data;

import java.math.BigDecimal;
@Data
public class DepositData {

    private String referenceId;
    private String transactionId;
    private String username;
    private String currencyCode;
    private BigDecimal beforeBalance;
    private BigDecimal afterBalance;
    private BigDecimal transferAmount;
    private Long timestamp;
}
