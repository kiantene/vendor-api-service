package com.nextgen.gameaggregator.entity.couchbase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.enums.TxnType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GameTransaction {

    @JsonProperty("idx")
    private Integer idx;

    @JsonProperty("type")
    private TxnType type;

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

    @JsonProperty("balance")
    @JsonSerialize(using = ToStringSerializer.class) // to avoid loss of precision
    @JsonDeserialize(using = NumberDeserializers.BigDecimalDeserializer.class)
    private BigDecimal balance;

    @JsonProperty("gaBetId")
    private String gaBetId;

    @JsonProperty("status")
    private TxnStatus status;

    @JsonProperty("state")
    private GameRoundState state;

    @JsonProperty("betTime")
    private Long betTime;

    @JsonProperty("settleTime")
    private Long settleTime;

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("sentAt")
    private String sentAt;

    @JsonProperty("doneAt")
    private String doneAt;

    public GameTransaction() {
        this.status = TxnStatus.NEW;
        this.state = GameRoundState.UNSETTLED;
    }

    public static GameTransaction of(TxnType type, Integer vendorId, String transactionId) {
        return new GameTransaction(type, vendorId, transactionId);
    }

    public GameTransaction(TxnType type, Integer vendorId, String transactionId) {
        this();
        this.type = type;
        this.vendorId = vendorId;
        this.transactionId = transactionId;
    }

    @JsonIgnore
    public String getId() {
        return vendorId + "::" + transactionId;
    }

    @JsonIgnore
    public String getRoundDocId() {
        return vendorId + "::" + roundId;
    }

    @JsonIgnore
    public boolean isSuccess() {
        return status == TxnStatus.SUCCESS;
    }
}
