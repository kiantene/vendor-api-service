package com.nextgen.gameaggregator.custodianseamless.operator.getsingletransaction;

import com.nextgen.gameaggregator.custodianseamless.constant.TransactionStatus;
import com.nextgen.gameaggregator.custodianseamless.constant.TransactionType;
import com.nextgen.gameaggregator.entity.ga.Currency;
import com.nextgen.gameaggregator.entity.wallet.TransferHistory;
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

    public GetSingleTransactionData(TransferHistory transferHistory, Currency currency){
        this.referenceId = transferHistory.getReferenceId();
        this.transactionId = transferHistory.getId();
        this.transactionStatus =
                TransactionStatus.getDescriptionByStatus(transferHistory.getTransactionStatus());
        this.transactionType = TransactionType.getTransactionTypeByStatus(transferHistory.getTransactionType());
        this.username = transferHistory.getAgentPlayerUsername();
        this.currencyCode = currency.getCode();
        this.beforeBalance = transferHistory.getBalanceBefore();
        this.afterBalance = transferHistory.getBalanceAfter();
        this.transferAmount = transferHistory.getTransferAmount();
        this.timestamp = transferHistory.getCreateTime();

    }

}