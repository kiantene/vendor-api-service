package com.nextgen.gameaggregator.vendor.data.couchbase.entity.seamlessbethistorycollection;

import org.springframework.data.annotation.Id;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

import java.math.BigDecimal;
import java.util.UUID;

@Document
@Scope("log")
@Collection("seamless_bet_history_collection")
public class SeamlessBetHistoryCollection {

    @Id
    private String id;
    private BigDecimal betAmount;
    private UUID betHistoryId;
    private Long betTime;
    private Long receivedTime;
    private String vendorRoundId;

    public SeamlessBetHistoryCollection() {

    }

    public SeamlessBetHistoryCollection(String id, BigDecimal betAmount, UUID betHistoryId, Long betTime, Long receivedTime, String vendorRoundId) {
        this.id = id;
        this.betAmount = betAmount;
        this.betHistoryId = betHistoryId;
        this.betTime = betTime;
        this.receivedTime = receivedTime;
        this.vendorRoundId = vendorRoundId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BigDecimal getBetAmount() {
        return betAmount;
    }

    public void setBetAmount(BigDecimal betAmount) {
        this.betAmount = betAmount;
    }

    public UUID getBetHistoryId() {
        return betHistoryId;
    }

    public void setBetHistoryId(UUID betHistoryId) {
        this.betHistoryId = betHistoryId;
    }

    public Long getBetTime() {
        return betTime;
    }

    public void setBetTime(Long betTime) {
        this.betTime = betTime;
    }

    public Long getReceivedTime() {
        return receivedTime;
    }

    public void setReceivedTime(Long receivedTime) {
        this.receivedTime = receivedTime;
    }

    public String getVendorRoundId() {
        return vendorRoundId;
    }

    public void setVendorRoundId(String vendorRoundId) {
        this.vendorRoundId = vendorRoundId;
    }
}
