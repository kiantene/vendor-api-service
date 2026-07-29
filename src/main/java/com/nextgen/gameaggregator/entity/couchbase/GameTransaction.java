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

    @JsonProperty("effectiveTurnover")
    @JsonSerialize(using = ToStringSerializer.class) // to avoid loss of precision
    @JsonDeserialize(using = NumberDeserializers.BigDecimalDeserializer.class)
    private BigDecimal effectiveTurnover;

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
        super();
        this.status = TxnStatus.NEW;
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
        if (isRollback()) {
            this.state = GameRoundState.PENDING;
        }
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
        copy.jackpotAmount = this.jackpotAmount;
        copy.effectiveTurnover = this.effectiveTurnover;
        // capped* live on RoundTxn (inherited); copy them so a copied capped txn stays capped
        copy.setCappedWinAmount(this.getCappedWinAmount());
        copy.setCappedJackpotAmount(this.getCappedJackpotAmount());
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

    public static String createDocId(String className, TxnType type, String transactionId) {
        return className + "::" + type + "::" + transactionId;
    }

    @Override
    @JsonIgnore
    public String getId() {
        return createDocId(className, type, transactionId);
    }

    /**
     * Overridden because GameTransaction shadows winAmount/jackpotAmount with its own private fields;
     * the RoundTxn version would read the (unset) superclass fields on a GameTransaction. Read via the
     * getters so the coalesce uses this instance's own amounts.
     */
    @Override
    @JsonIgnore
    public BigDecimal cappedWinAmountOrVendor() {
        return getCappedWinAmount() != null ? getCappedWinAmount() : getWinAmount();
    }

    @Override
    @JsonIgnore
    public BigDecimal cappedJackpotAmountOrVendor() {
        return getCappedJackpotAmount() != null ? getCappedJackpotAmount() : getJackpotAmount();
    }

    @JsonIgnore
    public String getRoundDocId() {
        return className + "::" + username + "::" + roundId;
    }

    @JsonIgnore
    public String getRollbackId() {
        return className + "::" + TxnType.BET + "::" + vendorBetId;
    }
}
