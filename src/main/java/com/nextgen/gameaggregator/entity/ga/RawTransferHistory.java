package com.nextgen.gameaggregator.entity.ga;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.custodianseamless.constant.TransactionStatus;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

import java.math.BigDecimal;
import java.util.UUID;


@Document
@Scope("raw")
@TypeAlias("transfer_histories")
@Collection("transfer_histories")
@Data
public class RawTransferHistory {
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

    public RawTransferHistory(){

    }

    //create transaction history before call to wallet service
    public RawTransferHistory
    (String referenceId, AgentPlayer agentPlayer, Currency currency, Integer transactionType, BigDecimal transferAmount) {
        this.id = String.valueOf(UUID.randomUUID());
        this.referenceId = referenceId;
        this.walletTransactionId = null;
        this.agentId = agentPlayer.getAgentId();
        this.agentPlayerId = agentPlayer.getId();
        this.agentPlayerUsername = agentPlayer.getUsername();
        this.currencyId = currency.getId();
        this.transactionStatus = TransactionStatus.PROCESSING.status;
        this.transactionType = transactionType;
        this.balanceBefore = null;
        this.balanceAfter = null;
        this.transferAmount = transferAmount;
        this.errorCode = ResponseCodes.Status.SC_TRANSACTION_STILL_PROCESSING.code;
        this.resultTime = null;
        this.createTime= System.currentTimeMillis();
    }


}
