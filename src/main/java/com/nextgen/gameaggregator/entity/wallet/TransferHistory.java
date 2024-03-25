package com.nextgen.gameaggregator.entity.wallet;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.entity.ga.RawTransferHistory;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "transfer_history")
@Data
@NoArgsConstructor
public class TransferHistory {

    @Id
    @JsonProperty("id")
    private String id;
    @JsonProperty("reference_id")
    private String referenceId;
    @JsonProperty("wallet_transaction_id")
    private String walletTransactionId;
    @JsonProperty("agent_id")
    private Integer agentId;
    @JsonProperty("agent_player_id")
    private Long agentPlayerId;
    @JsonProperty("agent_player_username")
    private String agentPlayerUsername;
    @JsonProperty("currency_id")
    private Integer currencyId;
    @JsonProperty("transaction_status")
    private Integer transactionStatus;
    @JsonProperty("transaction_type")
    private Integer transactionType;
    @JsonProperty("balance_before")
    private BigDecimal balanceBefore;
    @JsonProperty("balance_after")
    private BigDecimal balanceAfter;
    @JsonProperty("transfer_amount")
    private BigDecimal transferAmount;
    @JsonProperty("error_code")
    private Integer errorCode;
    @JsonProperty("result_time")
    private Long resultTime;
    @JsonProperty("create_time")
    private Long createTime;

    public TransferHistory
            (RawTransferHistory rawTransferHistory) {
        this.id = rawTransferHistory.getTransferHistoryId();
        this.referenceId = rawTransferHistory.getReferenceId();
        this.walletTransactionId = rawTransferHistory.getWalletTransactionId();
        this.agentId = rawTransferHistory.getAgentId();
        this.agentPlayerId = rawTransferHistory.getAgentPlayerId();
        this.agentPlayerUsername = rawTransferHistory.getAgentPlayerUsername();
        this.currencyId = rawTransferHistory.getCurrencyId();
        this.transactionStatus = rawTransferHistory.getTransactionStatus();
        this.transactionType = rawTransferHistory.getTransactionType();
        this.balanceBefore = rawTransferHistory.getBalanceBefore();
        this.balanceAfter = rawTransferHistory.getBalanceAfter();
        this.transferAmount = rawTransferHistory.getTransferAmount();
        this.errorCode = rawTransferHistory.getErrorCode();
        this.resultTime = rawTransferHistory.getResultTime();
        this.createTime= rawTransferHistory.getCreateTime();
    }


}