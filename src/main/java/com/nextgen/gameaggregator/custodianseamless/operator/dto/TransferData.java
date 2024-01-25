package com.nextgen.gameaggregator.custodianseamless.operator.dto;

import com.nextgen.gameaggregator.entity.ga.RawTransferHistory;
import lombok.Data;

import java.math.BigDecimal;
@Data
public class TransferData {

    private String referenceId;
    private String transactionId;
    private String username;
    private String currencyCode;
    private BigDecimal beforeBalance;
    private BigDecimal afterBalance;
    private BigDecimal transferAmount;
    private Long timestamp;

    public TransferData(RawTransferHistory rawTransferHistory, String currencyCode){
        this.referenceId = rawTransferHistory.getId();
        this.transactionId = rawTransferHistory.getTransactionId();
        this.username = rawTransferHistory.getAgentPlayerUsername();
        this.currencyCode = currencyCode;
        this.beforeBalance = rawTransferHistory.getBalanceBefore();
        this.afterBalance = rawTransferHistory.getBalanceAfter();
        this.transferAmount =rawTransferHistory.getTransferAmount();
        this.timestamp = rawTransferHistory.getCreateTime();
    }
}
