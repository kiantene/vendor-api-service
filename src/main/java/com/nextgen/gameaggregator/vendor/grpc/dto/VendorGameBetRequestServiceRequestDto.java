package com.nextgen.gameaggregator.vendor.grpc.dto;

import java.math.BigDecimal;

public class VendorGameBetRequestServiceRequestDto {
    private String hash;
    private String userId;
    private String gameId;
    private String roundId;
    private BigDecimal amount;
    private String reference;
    private String providerId;
    private long timestamp;
    private String roundDetails;

    public VendorGameBetRequestServiceRequestDto() {
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public String getRoundId() {
        return roundId;
    }

    public void setRoundId(String roundId) {
        this.roundId = roundId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getRoundDetails() {
        return roundDetails;
    }

    public void setRoundDetails(String roundDetails) {
        this.roundDetails = roundDetails;
    }
}
