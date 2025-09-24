package com.nextgen.gameaggregator.entity.couchbase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.nextgen.core.util.UuidUtil;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.enums.TxnType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Data
@EqualsAndHashCode(callSuper = true)
public class GameTransaction extends RoundTxn {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneOffset.UTC);

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("state")
    private GameRoundState state;

    @JsonProperty("className")
    private String className;

    @JsonProperty("idx")
    private Integer idx;

    /**
     * Vendor's request unique identifier to prevent duplicate transaction
     */
    @JsonProperty("transactionId")
    private String transactionId;

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

    @JsonProperty("balance")
    @JsonSerialize(using = ToStringSerializer.class) // to avoid loss of precision
    @JsonDeserialize(using = NumberDeserializers.BigDecimalDeserializer.class)
    private BigDecimal balance;

    @JsonProperty("betTime")
    private Long betTime;

    @JsonProperty("settleTime")
    private Long settleTime;

    @JsonProperty("createdTs")
    private Long createdTs;

    public GameTransaction() {
        this.status = TxnStatus.NEW;
        this.state = GameRoundState.UNSETTLED;
    }

    public static GameTransaction of(TxnType type, String vendorClassName, String transactionId, long createdTimestamp) {
        GameTransaction txn = new GameTransaction(type, vendorClassName, transactionId);
        txn.setGaBetId(UuidUtil.newUuidV7String());
        txn.setCreatedTs(createdTimestamp);
        txn.setCreatedAt(DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(createdTimestamp)));

        return txn;
    }

    public GameTransaction(TxnType type, String className, String transactionId) {
        this();
        this.type = type;
        this.className = className;
        this.transactionId = transactionId;
    }

    public GameTransaction copy() {
        GameTransaction copy = new GameTransaction();
        copy.createdAt = this.createdAt;
        copy.state = this.state;
        copy.className = this.className;
        copy.idx = this.idx;
        copy.type = this.type;
        copy.transactionId = this.transactionId;
        copy.vendorBetId = this.vendorBetId;
        copy.vendorId = this.vendorId;
        copy.roundId = this.roundId;
        copy.username = this.username;
        copy.gameCode = this.gameCode;
        copy.currency = this.currency;
        copy.betAmount = this.betAmount;
        copy.winAmount = this.winAmount;
        copy.balance = this.balance;
        copy.gaBetId = this.gaBetId;
        copy.status = this.status;
        copy.betTime = this.betTime;
        copy.settleTime = this.settleTime;
        copy.sentAt = this.sentAt;
        copy.doneAt = this.doneAt;
        copy.createdTs = this.createdTs;
        return copy;
    }

    @Override
    @JsonIgnore
    public String getId() {
        return className + "::" + type + "::" + transactionId;
    }

    @JsonIgnore
    public String getRoundDocId() {
        return className + "::" + roundId;
    }

    @JsonIgnore
    public String getRollbackId() {
        return className + "::" + TxnType.BET + "::" + vendorBetId;
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
}
