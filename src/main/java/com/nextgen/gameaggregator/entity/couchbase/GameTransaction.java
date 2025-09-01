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
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Data
public class GameTransaction {
    private static final DateTimeFormatter UTC_TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneOffset.UTC);

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
        this.createdAt = UTC_TIMESTAMP_FMT.format(Instant.now());
    }

    public static GameTransaction of(Integer vendorId, String transactionId) {
        return new GameTransaction(vendorId, transactionId);
    }

    public GameTransaction(Integer vendorId, String transactionId) {
        super();
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
