package com.nextgen.gameaggregator.entity.couchbase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.nextgen.gameaggregator.enums.TxnStatus;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GameTransaction {
    @JsonProperty("type")
    private String type;

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

    @JsonProperty("status")
    private TxnStatus status;

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
    }

    public static GameTransaction of(String type, Integer vendorId, String transactionId) {
        return new GameTransaction(type, vendorId, transactionId);
    }

    public GameTransaction(String type, Integer vendorId, String transactionId) {
        super();
        this.type = type;
        this.vendorId = vendorId;
        this.transactionId = transactionId;
    }

    @JsonIgnore
    public String getId() {
        return vendorId + "::" + transactionId;
    }

    public boolean isSuccess() {
        return status == TxnStatus.SUCCESS;
    }
}
