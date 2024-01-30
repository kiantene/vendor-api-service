package com.nextgen.gameaggregator.entity.wallet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "transfer_history")
@Data
@NoArgsConstructor
public class TransferHistory {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    @JsonProperty("id")
    private String id;

    @Column(name = "reference_id", length = 36, nullable = false)
    @JsonProperty("reference_id")
    private String referenceId;

    @Column(name = "wallet_transaction_id", length = 36)
    @JsonProperty("wallet_transaction_id")
    private String walletTransactionId;

    @Column(name = "agent_id", nullable = false)
    @JsonProperty("agent_id")
    private int agentId;

    @Column(name = "agent_player_id", nullable = false)
    @JsonProperty("agent_player_id")
    private long agentPlayerId;

    @Column(name = "agent_player_username", length = 100, nullable = false)
    @JsonProperty("agent_player_username")
    private String agentPlayerUsername;

    @Column(name = "currency_id", nullable = false)
    @JsonProperty("currency_id")
    private int currencyId;

    @Column(name = "transaction_status", nullable = false)
    @JsonProperty("transaction_status")
    private int transactionStatus;

    @Column(name = "transaction_type", nullable = false)
    @JsonProperty("transaction_type")
    private int transactionType;

    @Column(name = "before_balance", precision = 20, scale = 8)
    @JsonProperty("before_balance")
    private Double beforeBalance;

    @Column(name = "after_balance", precision = 20, scale = 8)
    @JsonProperty("after_balance")
    private Double afterBalance;

    @Column(name = "transfer_amount", precision = 20, scale = 8, nullable = false)
    @JsonProperty("transfer_amount")
    private Double transferAmount;

    @Column(name = "error_type")
    @JsonProperty("error_type")
    private Integer errorType;

    @Column(name = "result_time", nullable = false)
    @JsonProperty("result_time")
    private long resultTime;

    @Column(name = "create_time", nullable = false)
    @JsonProperty("create_time")
    private long createTime;

}