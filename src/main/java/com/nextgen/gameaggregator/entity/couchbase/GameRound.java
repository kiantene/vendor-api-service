package com.nextgen.gameaggregator.entity.couchbase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.nextgen.gameaggregator.enums.GameRoundState;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameRound {

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("state")
    private GameRoundState state;

    @JsonProperty("className")
    private String className;

    @JsonProperty("vendorId")
    private Integer vendorId;

    @JsonProperty("roundId")
    private String roundId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("gameCode")
    private String gameCode;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("lastBalance")
    @JsonSerialize(using = ToStringSerializer.class) // to avoid loss of precision
    @JsonDeserialize(using = NumberDeserializers.BigDecimalDeserializer.class)
    private BigDecimal lastBalance;

    @JsonProperty("betAmount")
    @JsonSerialize(using = ToStringSerializer.class) // to avoid loss of precision
    @JsonDeserialize(using = NumberDeserializers.BigDecimalDeserializer.class)
    private BigDecimal betAmount;

    @JsonProperty("winAmount")
    @JsonSerialize(using = ToStringSerializer.class) // to avoid loss of precision
    @JsonDeserialize(using = NumberDeserializers.BigDecimalDeserializer.class)
    private BigDecimal winAmount;

    @JsonProperty("jackpotAmount")
    @JsonSerialize(using = ToStringSerializer.class) // to avoid loss of precision
    @JsonDeserialize(using = NumberDeserializers.BigDecimalDeserializer.class)
    private BigDecimal jackpotAmount;

    @JsonProperty("agentMeta")
    private AgentMeta agentMeta;

    @JsonProperty("txnCount")
    private Integer txnCount;

    @JsonProperty("betTxnCount")
    private Integer betTxnCount;

    @JsonProperty("transactions")
    private List<RoundTxn> transactions;

    @JsonProperty("isEnded")
    private Boolean isEnded;

    @JsonProperty("createdTs")
    private Long createdTs;

    public GameRound() {
        this.txnCount = 1;
        this.betTxnCount = 0;
        this.state = GameRoundState.UNSETTLED;
        this.betAmount = BigDecimal.ZERO;
        this.winAmount = BigDecimal.ZERO;
        this.jackpotAmount = BigDecimal.ZERO;
        this.isEnded = false;
    }

    public GameRound(String className, String username, String roundId) {
        this();
        this.className = className;
        this.username = username;
        this.roundId = roundId;
    }

    public static GameRound of(String className, String username, String roundId) {
        return new GameRound(className, username, roundId);
    }

    @JsonIgnore
    public String getId() {
        return className + "::" + username + "::" + roundId;
    }

    @JsonIgnore
    public boolean isSettled() {
        return state == GameRoundState.SETTLED;
    }

    @JsonIgnore
    public boolean isUnsettled() {
        return state == GameRoundState.UNSETTLED;
    }

    @JsonIgnore
    public boolean isRefunded() {
        return state == GameRoundState.REFUNDED;
    }

    @JsonIgnore
    public boolean hasMultipleBets() {
        return betTxnCount != null && betTxnCount > 1;
    }

    @JsonIgnore
    public boolean isEnded() {
        return Boolean.TRUE.equals(isEnded);
    }

    public void setBetAmount(BigDecimal betAmount) {
        this.betAmount = (betAmount == null) ? BigDecimal.ZERO : betAmount;
    }

    public void setWinAmount(BigDecimal winAmount) {
        this.winAmount = (winAmount == null) ? BigDecimal.ZERO : winAmount;
    }

    public void setTransactions(List<RoundTxn> transactions) {
        if (transactions == null) return;

        this.transactions = transactions;
        this.txnCount = transactions.size();
    }
}
