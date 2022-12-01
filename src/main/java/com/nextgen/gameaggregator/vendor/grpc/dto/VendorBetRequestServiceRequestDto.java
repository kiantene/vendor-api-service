package com.nextgen.gameaggregator.vendor.grpc.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VendorBetRequestServiceRequestDto {
    private String hash;
    private String userId;
    private String gameId;
    private String roundId;
    private String amount;
    private String reference;
    private String providerId;
    private String timestamp;
    private String roundDetails;
    private String token;

    public VendorBetRequestServiceRequestDto() {

    }

    public VendorBetRequestServiceRequestDto(String hash, String userId, String gameId, String roundId, String amount, String reference, String providerId, String timestamp, String roundDetails, String token) {
        this.hash = hash;
        this.userId = userId;
        this.gameId = gameId;
        this.roundId = roundId;
        this.amount = amount;
        this.reference = reference;
        this.providerId = providerId;
        this.timestamp = timestamp;
        this.roundDetails = roundDetails;
        this.token = token;
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

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
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

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getRoundDetails() {
        return roundDetails;
    }

    public void setRoundDetails(String roundDetails) {
        this.roundDetails = roundDetails;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
