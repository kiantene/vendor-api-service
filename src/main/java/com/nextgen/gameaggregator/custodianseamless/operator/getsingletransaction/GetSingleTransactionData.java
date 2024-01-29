package com.nextgen.gameaggregator.custodianseamless.operator.getsingletransaction;

import com.nextgen.gameaggregator.custodianseamless.constant.TransactionStatus;
import com.nextgen.gameaggregator.custodianseamless.constant.TransactionType;
import com.nextgen.gameaggregator.entity.ga.Currency;
import com.nextgen.gameaggregator.entity.ga.RawTransferHistory;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GetSingleTransactionData {

    private String referenceId;
    private String transactionId;
    private String transactionStatus;
    private String transactionType;
    private String username;
    private String currencyCode;
    private BigDecimal beforeBalance;
    private BigDecimal afterBalance;
    private BigDecimal transferAmount;
    private Long timestamp;

    public GetSingleTransactionData(RawTransferHistory rawTransferHistory, Currency currency){
        this.referenceId = rawTransferHistory.getId();
        this.transactionId = rawTransferHistory.getTransactionId();
        this.transactionStatus =
                TransactionStatus.getDescriptionByStatus(rawTransferHistory.getTransactionStatus());
        this.transactionType = TransactionType.getTransactionTypeByStatus(rawTransferHistory.getTransactionType());
        this.username = rawTransferHistory.getAgentPlayerUsername();
        this.currencyCode = currency.getCode();
        this.beforeBalance = rawTransferHistory.getBalanceBefore();
        this.afterBalance = rawTransferHistory.getBalanceAfter();
        this.transferAmount = rawTransferHistory.getTransferAmount();
        this.timestamp = rawTransferHistory.getCreateTime();

    }

}
